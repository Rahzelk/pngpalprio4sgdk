import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.plaf.basic.BasicButtonUI;


public class ImagePanel extends JPanel implements MouseWheelListener  {
    private final ImageHandler imageHandler;
    private final PngPalettePriorityEditor mainWindow;
    private double zoom;
    private final Set<Point> selectedTiles = new HashSet<>();
    private Point selectionStart = null;
    private Point selectionEnd = null;

    private VolatileImage  tileOverlayBuffer = null;
    private boolean bufferNeedsUpdate = true;

    private final Stack<Mask> undoStack = new Stack<>();
    private static final int UNDO_LIMIT = 10;

    private MessageHandler messageHandler = null;
    boolean isCtrlPressed = false;

    private  Color GRID_BORDER_COLOR;
    private  Color GRID_HIGH_PRIORITY_BORDER_COLOR;
    private  Color GRID_SELECTED_TILE_COLOR;
    private  Color GRID_SELECTION_LASSO_COLOR;
    
    private final Color GRID_PALETTE_INDEX_COLORS_TILE[] = new Color[4];
    private Color GRID_PALETTE_INDEX_COLORS_TEXT;





    public ImagePanel(PngPalettePriorityEditor mainFrame) {

        this.mainWindow = mainFrame;

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {       
                handleKeyPress(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) { // Left Click (Start Selection)
                    isCtrlPressed = e.isControlDown();
                    selectionStart = e.getPoint();
                    selectionEnd = selectionStart;

                } else if (e.getButton() == MouseEvent.BUTTON3) { // Right Click (Open Properties)
                    openTilePropertiesDialog();
                }
            }

        
            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionStart != null) {
                    applySelection();
                }
            }
        });



        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursorPosition(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateCursorPosition(e.getX(), e.getY());

                if (selectionStart != null) {
                    selectionEnd = e.getPoint();
                    updateSelectionStatus(getCurrentCursorText());
                    repaint();  //update selecting rectangle
                }
            }
        });


        zoom = 1;
        AppFileHandler.loadConfig(); 
        updateColors(AppFileHandler.loadColorsFromConfig());
        imageHandler = new ImageHandler();
        setBackground(Color.GRAY);
        setPreferredSize(new Dimension(800, 600));       

        setFocusable(true);
        requestFocusInWindow();

    }
    
    private void updateCursorPosition(int x, int y) {
        
        if(!assetsLoaded() || zoom<1)  return;

         // Récupérer les valeurs de scrolling
        JViewport viewport = (JViewport) getParent();
        Point viewPosition = viewport.getViewPosition();
        
        // Calculer la position en pixel réelle dans l'image
        int pixelX = (int) ((x + viewPosition.x) / zoom);
        int pixelY = (int) ((y + viewPosition.y) / zoom);
        
        int tileSize = (int)(ImageHandler.TILE_SIZE * zoom);

        int tileX = x / tileSize;
        int tileY = y / tileSize;
    
        String cursorText = String.format("Cursor: px(%d,%d) | tile(%d, %d)", pixelX, pixelY, tileX, tileY);
    
        // Conserver l'affichage de la sélection si elle existe
        if (selectedTiles != null && !selectedTiles.isEmpty()) {
            updateSelectionStatus(cursorText);
        } else {
            mainWindow.updateStatusBar(cursorText);
        }
    }

    private void updateSelectionStatus(String cursorText) {
        if (!assetsLoaded()) return;

        if(zoom<1)  
        {
            mainWindow.updateStatusBar("unavailable at this zoom level");
            return;
        }

        if (selectedTiles == null || selectedTiles.isEmpty()) {
            mainWindow.updateStatusBar(cursorText);
            return;
        }

        Rectangle bounds = getSelectionBounds();
    
        if (bounds != null) {
            mainWindow.updateStatusBar(String.format(
                "%s | Selection: (%d, %d) → (%d, %d) | %d x %d tiles",
                cursorText, bounds.x, bounds.y, 
                bounds.x + bounds.width - 1, bounds.y + bounds.height - 1,
                bounds.width, bounds.height
            ));
        }    
        else if (selectedTiles.size() == 1) {
            Point tile =(Point)  selectedTiles.toArray()[0];
            mainWindow.updateStatusBar(
                String.format("%s | Tile Selected : (%d, %d) | 1 tile", cursorText, tile.x, tile.y));
        } else {
            mainWindow.updateStatusBar(
                String.format("%s | Multi-selection: %d tiles selected", cursorText, selectedTiles.size()));
        }
    }    


    private Rectangle getSelectionBounds() {
        if (selectedTiles == null || selectedTiles.isEmpty()) {
            return null;
        }
    
        // Trouver les min/max X et Y
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
    
        for (Point tile : selectedTiles) {
            if (tile.x < minX) minX = tile.x;
            if (tile.y < minY) minY = tile.y;
            if (tile.x > maxX) maxX = tile.x;
            if (tile.y > maxY) maxY = tile.y;
        }
    
        // Vérifier si la sélection est un rectangle complet
        int width = (maxX - minX) + 1;
        int height = (maxY - minY) + 1;
    
        if (selectedTiles.size() == (width * height)) {
            return new Rectangle(minX, minY, width, height);
        }
    
        return null; // Pas un rectangle parfait
    }

    // Récupérer la position actuelle du curseur
    private String getCurrentCursorText() {
        if (!assetsLoaded()) return "";

        if(zoom<1)  
        {
            return "unavailable at this zoom level";
        }
        
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point location = pointerInfo.getLocation();
        SwingUtilities.convertPointFromScreen(location, this);
        int tileSize = (int)(ImageHandler.TILE_SIZE * zoom);

        int tileX = location.x  / tileSize;
        int tileY = location.y / tileSize;
    
        return String.format("Cursor: (%d px, %d px) | (%d tile, %d tile)", location.x, location.y, tileX, tileY);
    }


    private void applySelection() {
       
        if (selectionStart == null || selectionEnd == null) return;
    
        int tileSize = (int)(ImageHandler.TILE_SIZE * zoom);
        
        int startX = Math.min(selectionStart.x, selectionEnd.x) / tileSize;
        int startY = Math.min(selectionStart.y, selectionEnd.y) / tileSize;
        int endX = Math.max(selectionStart.x, selectionEnd.x) / tileSize;
        int endY = Math.max(selectionStart.y, selectionEnd.y) / tileSize;
    
        // Si CTRL n'est pas enfoncé, on efface la sélection existante
        if (!isCtrlPressed) {
            selectedTiles.clear();
        }
        
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                selectedTiles.add(new Point(x, y));
            }
        }
        updateSelectionStatus( getCurrentCursorText() );
        selectionStart = null;
        selectionEnd = null;
        bufferNeedsUpdate = true;
        repaint();
    }




    public void loadImage() 
    {
        BufferedImage loadedImage = AppFileHandler.selectInputImage(this);
        if (loadedImage != null) 
        {
            int returnCode = imageHandler.setImage(loadedImage);  
            
            if(returnCode ==0) 
            {
                setPreferredSize(new Dimension(
                    (int)(imageHandler.getImage().getWidth() * zoom), 
                    (int)(imageHandler.getImage().getHeight() * zoom)));

                
                revalidate();
                repaint();             

                mainWindow.allowMenuChoice();
            }
            else{
                this.messageHandler= new MessageHandler(returnCode,ImageHandler.getErrorMessage(returnCode), JOptionPane.ERROR_MESSAGE);
                showMessage();            
            }
        }


    }



    private void showMessage()
    {
    String title = "Information";
    
    if (messageHandler.getType() == JOptionPane.ERROR_MESSAGE){
        title = "Information";
    }
    

    JOptionPane.showMessageDialog(this,  
                                messageHandler.getMessage() +" (Code #"+messageHandler.getCode()+")" , 
                                title,
                                messageHandler.getType() );   
    }


            
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) 
    {
        if (!assetsLoaded()) return;

        int rotation = e.getWheelRotation();

        if (e.isShiftDown()) 
        { // zoom works with SHIFT + Wheel

            if (rotation < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
           
            bufferNeedsUpdate = true;
            setPreferredSize(new Dimension(
                (int)(imageHandler.getImage() .getWidth() * zoom), 
                (int)(imageHandler.getImage() .getHeight() * zoom)));
            revalidate();
            repaint();

            e.consume();
        }
        else
        {
            getParent().dispatchEvent(e);
            JScrollPane parent = (JScrollPane) getParent().getParent();
            JViewport viewport = parent.getViewport();
            Point viewPosition = viewport.getViewPosition();
    
            int scrollAmount = (int) (24  * zoom);

            if (rotation < 0) {
                viewPosition.y = Math.max(viewPosition.y - scrollAmount, 0);
            } else {
                viewPosition.y = Math.min(viewPosition.y + scrollAmount, getHeight() - viewport.getHeight());
            }  

            bufferNeedsUpdate = true;
            viewport.setViewPosition(viewPosition);          
            repaint();                        
        }
        
    }


    private void drawTileOverlay(Graphics g) {
        if (!assetsLoaded()) return;
    
        if ( !drawSelectionRectangle(g) && bufferNeedsUpdate) {
            updateTileOverlayBuffer();
            bufferNeedsUpdate = false;
        }
      
        if (tileOverlayBuffer != null) {
            g.drawImage(tileOverlayBuffer, 0, 0, null);
        }
    }

    private boolean  drawSelectionRectangle(Graphics g) {
        if (selectionStart == null || selectionEnd == null) return false;
        
        int rectX = Math.min(selectionStart.x, selectionEnd.x);
        int rectY = Math.min(selectionStart.y, selectionEnd.y);
        int rectWidth = Math.max(selectionStart.x, selectionEnd.x) - rectX;
        int rectHeight = Math.max(selectionStart.y, selectionEnd.y) - rectY;
    
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(GRID_SELECTION_LASSO_COLOR);
        g2d.fillRect(rectX, rectY, rectWidth, rectHeight);
        return true;
    }

    public void doScrollbarUpdate()
    {
        bufferNeedsUpdate = true;
        repaint();
    }


    private void allocateVolatileImage(int width, int height) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (tileOverlayBuffer != null) {
            tileOverlayBuffer.flush(); // Libérer l'ancienne image
        }
        tileOverlayBuffer = gc.createCompatibleVolatileImage(width, height, Transparency.TRANSLUCENT);
    }
    

    private void updateTileOverlayBuffer() {
        if (!assetsLoaded()) return;
        
        int tileSize = (int)(ImageHandler.TILE_SIZE * zoom);
        Mask mask = imageHandler.getMask();
        int imageWidth = mask.getWidth() * tileSize;
        int imageHeight = mask.getHeight() * tileSize;
    
        
            // Vérifier si l'image doit être recréée
        if (tileOverlayBuffer == null || 
            tileOverlayBuffer.getWidth() != imageWidth || 
            tileOverlayBuffer.getHeight() != imageHeight ||
            tileOverlayBuffer.contentsLost()) {
            allocateVolatileImage(imageWidth, imageHeight);
        }
 
        do {
            Graphics2D g2d = tileOverlayBuffer.createGraphics();

            // 🔥 Correction du "fondu au blanc"
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, imageWidth, imageHeight);
            g2d.setComposite(AlphaComposite.SrcOver);

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);      

            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth("3");
            int textHeight = fm.getHeight();
            int textXCenter = (tileSize - textWidth) / 2;
            int textYCenter = (tileSize + textHeight) / 2 - 3;

            // Déterminer les tuiles visibles dans la fenêtre
            Rectangle visibleRect = getVisibleRect();
            int startX = Math.max(0, visibleRect.x / tileSize);
            int startY = Math.max(0, visibleRect.y / tileSize);
            int endX = Math.min(imageWidth, (visibleRect.x + visibleRect.width) / tileSize + 1);
            int endY = Math.min(imageHeight, (visibleRect.y + visibleRect.height) / tileSize + 1);
        

            // Parcourir uniquement les tuiles visibles
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    int drawX = x * tileSize;
                    int drawY = y * tileSize;
                    int paletteIndex = mask.getTilePalette(x, y);
                    int priority = mask.getTilePriority(x, y);

                    if(mainWindow.showGrid())
                    {
                        g2d.setColor(GRID_BORDER_COLOR);
                        g2d.drawRect(drawX, drawY, tileSize, tileSize);
                    }
                                        
                    // Sélection en surbrillance
                    if (selectedTiles.contains(new Point(x, y))) {
                        g2d.setColor(GRID_SELECTED_TILE_COLOR);
                        g2d.fillRect(drawX, drawY, tileSize, tileSize);
                    }

                    if (priority == 1) {
                        g2d.setColor(GRID_HIGH_PRIORITY_BORDER_COLOR);
                        g2d.drawRect(drawX, drawY, tileSize, tileSize);
                    }



                    // Coloration des cases en fonction de la palette
                    g2d.setColor(GRID_PALETTE_INDEX_COLORS_TILE[paletteIndex]);
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);
                    
                    if (zoom >= 1 && 
                    (   (paletteIndex > 0 && mainWindow.showPaletteIndex())
                     || (paletteIndex == 0 && mainWindow.viewPaletteZero())  ) 
                    )
                    {
                        // Affichage du numéro de palette avec contour noir
                        String paletteText = String.valueOf(paletteIndex);

                        int textX = drawX + textXCenter;
                        int textY = drawY + textYCenter;
                
                        // Couleur du texte selon l'index de palette
                        g2d.setColor(GRID_PALETTE_INDEX_COLORS_TEXT);
                        g2d.drawString(paletteText, textX, textY);
                    }                
                }
            }
        } while (tileOverlayBuffer.contentsLost()); // Recréer si l'image est perdue
        
    }

    private void openTilePropertiesDialog() 
    {
        if (!assetsLoaded()) return;
    
        // Get the first selected tile
        Point firstTile = selectedTiles.iterator().next();  
        Tile firstTileData = imageHandler.getMask().getTile(firstTile.x, firstTile.y);
    
        // Create the dialog
        Window parentFrame = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentFrame, "Edit Tile Properties", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new GridLayout(3, 1));
        // Centrer par rapport à la fenêtre principale
        

        // Panel for Palette Selection
        JPanel palettePanel = new JPanel();
        palettePanel.setBorder(BorderFactory.createTitledBorder("Palette Index"));
        ButtonGroup paletteGroup = new ButtonGroup();
        JRadioButton[] paletteButtons = new JRadioButton[5];
    
        for (int i = 0; i < 4; i++) {
            paletteButtons[i] = new JRadioButton(String.valueOf(i));
            paletteGroup.add(paletteButtons[i]);
            palettePanel.add(paletteButtons[i]);
        }
        paletteButtons[firstTileData.getPalette()].setSelected(true); // Select current palette
    
        // Panel for Priority Selection
        JPanel priorityPanel = new JPanel();
        priorityPanel.setBorder(BorderFactory.createTitledBorder("Priority"));
        ButtonGroup priorityGroup = new ButtonGroup();
        JRadioButton priorityLow = new JRadioButton("LOW");
        JRadioButton priorityHigh = new JRadioButton("HIGH");
        priorityGroup.add(priorityLow);
        priorityGroup.add(priorityHigh);
        priorityPanel.add(priorityLow);
        priorityPanel.add(priorityHigh);
    
        if (firstTileData.getPriority() == 1) {
            priorityHigh.setSelected(true);
        } else {
            priorityLow.setSelected(true);
        }
    
        // OK Button
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            int newPalette = -1;
            for (int i = 0; i < 5; i++) {
                if (paletteButtons[i].isSelected()) {
                    newPalette = i;
                    break;
                }
            }
            int newPriority = priorityHigh.isSelected() ? 1 : 0;
    
            // Apply changes to all selected tiles
            saveStateForUndo();
            for (Point p : selectedTiles) {
                imageHandler.getMask().setTileProperties(p.x, p.y, newPalette, newPriority);
            }
    
            dialog.dispose();
            repaint();
        });
    
        // Add panels to dialog
        dialog.add(palettePanel);
        dialog.add(priorityPanel);
        dialog.add(okButton);
    
        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }
    

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imageHandler.getImage() != null) {
            g.drawImage(imageHandler.getImage(), 0, 0, 
                        (int)(imageHandler.getImage().getWidth() * zoom), 
                        (int)(imageHandler.getImage().getHeight() * zoom), null);
        }
    
        drawTileOverlay(g);  
    }

    private boolean assetsLoaded()
    {
        return assetsLoaded(false);
    }
    private boolean assetsLoaded(boolean showDialog)
    {
        if (imageHandler.getImage()  == null || imageHandler.getMask() == null) {
            if(showDialog) {
                this.messageHandler= new MessageHandler(7,"No image or mask loaded.", JOptionPane.ERROR_MESSAGE);
                showMessage();
            }
            return false;
        }

        return true;
    }    



    public void exportImage() 
    {
        if (!assetsLoaded(true)) return;

        File outputFile = AppFileHandler.selectOutputImage(this);

        if(outputFile !=null)
        {
            imageHandler.applyMask();
            int returnCode = AppFileHandler.writeOutputImage(imageHandler.getExportedImage(), outputFile);
                
            if(returnCode>0)
                this.messageHandler= new MessageHandler(returnCode,"Failed to export image.", JOptionPane.ERROR_MESSAGE);    
            else
                this.messageHandler= new MessageHandler(returnCode,"Image exported successfully!.", JOptionPane.INFORMATION_MESSAGE);

            showMessage();                     
        }
    }

    
    public void saveMask() 
    {
        if (!assetsLoaded(true)) return;

        AppFileHandler.saveMask(this, imageHandler.getMask());
    }


    public void loadMask() 
    {
        Mask mask = AppFileHandler.loadMask(this);

        if(mask != null)
        {
            imageHandler.setMask(mask);
            repaint();
        }
    }
 

    private void zoomIn() {
        if (zoom < 6.0) { // max x6
            zoom += 0.5;
        }
    }
    
    private void zoomOut() {
        if (zoom > 0.5) { // Min x0.5
            zoom -= 0.5;
        }
    }


    public Mask getUndoStack()
    {
        if (!undoStack.isEmpty()) 
            return undoStack.pop();
        else
            return null;
    }

    public void saveStateForUndo() {
        // Copier le mask actuel pour l'empiler
        Mask maskCopy = new Mask(imageHandler.getMask());
    
        // Ajouter dans la pile
        undoStack.push(maskCopy);
    
        // Limiter à 5 actions max
        if (undoStack.size() > UNDO_LIMIT) {
            undoStack.remove(0); // Supprime l'état le plus ancien
        }
    }   
    
    private void undoLastAction() {
        Mask previousMask = getUndoStack();
        if (previousMask!=null) 
        {
            imageHandler.setMask(previousMask);
            repaint();
        }
    }


    public void handleKeyPress(KeyEvent e) 
    {
        if (!assetsLoaded()) return;   

        JScrollPane parent = (JScrollPane) getParent().getParent();
        JViewport viewport = parent.getViewport();
        Point viewPosition = viewport.getViewPosition();

        int scrollAmount = (int)(24 * zoom);
        int keyCode = e.getKeyCode();


        switch (keyCode) 
        {

            case KeyEvent.VK_Z -> { if (e.isControlDown()) undoLastAction(); }

            case KeyEvent.VK_LEFT -> viewPosition.x = Math.max(viewPosition.x - scrollAmount, 0);
            case KeyEvent.VK_RIGHT -> viewPosition.x = Math.min(viewPosition.x + scrollAmount, getWidth() - viewport.getWidth());
            case KeyEvent.VK_UP -> viewPosition.y = Math.max(viewPosition.y - scrollAmount, 0);
            case KeyEvent.VK_DOWN -> viewPosition.y = Math.min(viewPosition.y + scrollAmount, getHeight() - viewport.getHeight());

            case KeyEvent.VK_H -> {
                saveStateForUndo();
                // Met toutes les TILEs sélectionnées en priorité haute (1)                
                for (Point p : selectedTiles) {                    
                    imageHandler.getMask().setTileProperties(p.x, p.y, imageHandler.getMask().getTilePalette(p.x, p.y), 1);
                }
            }

            case KeyEvent.VK_L -> {
                saveStateForUndo();
                // Met toutes les TILEs sélectionnées en priorité basse (0)
                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, imageHandler.getMask().getTilePalette(p.x, p.y), 0);
                }
            }

            case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> {
                saveStateForUndo();
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 0;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }  

            case KeyEvent.VK_1,  KeyEvent.VK_NUMPAD1 -> {
                saveStateForUndo();
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 1;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }  

            case KeyEvent.VK_2,  KeyEvent.VK_NUMPAD2 -> {
                saveStateForUndo();
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 2;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }              

            
            case KeyEvent.VK_3,  KeyEvent.VK_NUMPAD3 -> {
                saveStateForUndo();
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 3;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }     

        }
        bufferNeedsUpdate = true;
        viewport.setViewPosition(viewPosition);
        revalidate();
        repaint();
    }



    private Color showRGBColorChooser(Component parent, Color initialColor) {
        // Crée une nouvelle instance de JColorChooser
        JColorChooser colorChooser = new JColorChooser(initialColor);
    
        // Supprime tous les onglets sauf "RGB"
        AbstractColorChooserPanel[] panels = colorChooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (!panel.getDisplayName().equals("RGB")) {
                colorChooser.removeChooserPanel(panel);
            }
        }
    
        // Crée un JDialog pour afficher le sélecteur
        JDialog dialog = JColorChooser.createDialog(
            parent,
            "Choose Color",
            true, // Modal
            colorChooser,
            null, // OK Button Listener (On gère ça manuellement)
            null  // Cancel Button Listener
        );
    
        dialog.setVisible(true);
    
        // Retourne la couleur sélectionnée
        return colorChooser.getColor();
    }



    public void openMaskColorSettings() {

      // Create the dialog
        Window parentFrame = SwingUtilities.getWindowAncestor(this);
        JDialog colorDialog = new JDialog(parentFrame, "Edit Tile Properties", Dialog.ModalityType.APPLICATION_MODAL);
        colorDialog.setLayout(new GridLayout(0, 2));


        String[] colorLabels = {
            "Array Border", "Selection Lasso", "Selected Tile", 
            "High Priority Border", "Palette Text",            
            "Palette 0", "Palette 1", "Palette 2", "Palette 3"
        };
    
        Color[] currentColors = {
            GRID_BORDER_COLOR, GRID_SELECTION_LASSO_COLOR, GRID_SELECTED_TILE_COLOR, 
            GRID_HIGH_PRIORITY_BORDER_COLOR,     
            GRID_PALETTE_INDEX_COLORS_TEXT,       
            GRID_PALETTE_INDEX_COLORS_TILE[0], GRID_PALETTE_INDEX_COLORS_TILE[1], 
            GRID_PALETTE_INDEX_COLORS_TILE[2], GRID_PALETTE_INDEX_COLORS_TILE[3]
        };
    
        JButton[] colorButtons = new JButton[currentColors.length];
    
        for (int i = 0; i < currentColors.length; i++) {
            final int index = i;
            JButton colorButton = new JButton();
            
            // Définir une couleur de fond et désactiver l'effet visuel de Swing
            //colorButton.setBackground(currentColors[index]);
            colorButton.setOpaque(true);
            colorButton.setBorderPainted(true);
            colorButton.setFocusPainted(false); // Supprime l'effet de focus
            colorButton.setUI(new BasicButtonUI()); // Force un UI basique sans animation
            
            Color displayColor = new Color(currentColors[index].getRed(), 
                               currentColors[index].getGreen(), 
                               currentColors[index].getBlue(), 
                               255); // Force l'alpha à 255

            colorButton.setBackground(displayColor);

            
            colorButton.addActionListener(e -> {

                Color newColor = showRGBColorChooser(colorDialog, currentColors[index]);
                
                if (newColor != null) {
                    currentColors[index] = newColor;

                    newColor = new Color(newColor.getRed(), 
                    newColor.getGreen(), 
                    newColor.getBlue(), 
                    255); // Force l'alpha à 255

                    colorButton.setBackground(newColor);

                }
            });

            colorButtons[i] = colorButton;
            colorDialog.add(new JLabel(colorLabels[index]));
            colorDialog.add(colorButton);
        }
    
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            AppFileHandler.saveColorsToConfig(currentColors);
            updateColors(currentColors);
            colorDialog.dispose();
        });
    
        JButton resetButton = new JButton("Reset to Default");
        resetButton.addActionListener(e -> {
            resetColorsToDefault();
            colorDialog.dispose();
        });
    
        colorDialog.add(saveButton);
        colorDialog.add(resetButton);
        colorDialog.pack();

        // Centrage proprement dit
        colorDialog.setLocationRelativeTo(parentFrame);
        

        colorDialog.setVisible(true);
    }



    private void updateColors(Color[] colors) {
        GRID_BORDER_COLOR = colors[0];
        GRID_SELECTION_LASSO_COLOR = colors[1];
        GRID_SELECTED_TILE_COLOR = colors[2];
        GRID_HIGH_PRIORITY_BORDER_COLOR = colors[3];
        GRID_PALETTE_INDEX_COLORS_TEXT = colors[4];
    
        for (int i = 0; i < GRID_PALETTE_INDEX_COLORS_TILE.length; i++) {
            GRID_PALETTE_INDEX_COLORS_TILE[i] = colors[i + 5];
        }
    
        repaint();
    }   
    

    public static  String colorToHex(Color color) {
        return String.format("#%02x%02x%02x%02x", color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
    }
    
    
    public static Color hexToColor(String hex) {
        return new Color(
            Integer.parseInt(hex.substring(3, 5), 16),
            Integer.parseInt(hex.substring(5, 7), 16),
            Integer.parseInt(hex.substring(7, 9), 16),
            Integer.parseInt(hex.substring(1, 3), 16)
        );
    }


    public static Color[] getDefaultColors()
    {
         Color[] defaultColors = {
            new Color(200, 200, 200, 50),    //GRID_BORDER_COLOR
            new Color(0, 255, 255, 200),     //GRID_SELECTION_LASSO_COLOR
            new Color(0, 255, 0, 200),       //GRID_SELECTED_TILE_COLOR
            new Color(0, 255, 255, 200),     //GRID_HIGH_PRIORITY_BORDER_COLOR
            new Color(255, 255, 255, 255),   //GRID_PALETTE_INDEX_COLORS_TEXT
            new Color(255, 255, 255, 100),   //GRID_PALETTE_INDEX_COLORS_TILE[0]
            new Color(255, 255, 0, 100),     //GRID_PALETTE_INDEX_COLORS_TILE[1]
            new Color(255, 0, 255, 100),     //GRID_PALETTE_INDEX_COLORS_TILE[2]
            new Color(0, 0, 255, 100)        //GRID_PALETTE_INDEX_COLORS_TILE[3]
        };  
        
        return defaultColors;
    }

    private void resetColorsToDefault() {

        updateColors(getDefaultColors());
    }    
}
