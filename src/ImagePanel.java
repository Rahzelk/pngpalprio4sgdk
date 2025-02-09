import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;


public class ImagePanel extends JPanel implements MouseWheelListener  {
    private final ImageHandler imageHandler;
    private int zoom = 1;
    private Set<Point> selectedTiles = new HashSet<>();
    private Point selectionStart = null;
    private Point selectionEnd = null;

    private MessageHandler messageHandler = null;

    private final Color GRID_HIGH_PRIORITY_ARRAY_COLOR = new Color(0, 0, 255);
    private final Color GRID_HIGH_PRIORITY_TILE_COLOR = new Color(255, 255, 255, 50);
    private final Color GRID_ARRAY_COLOR = new Color(200, 200, 200, 150);
    private final Color GRID_SELECTED_TILE_COLOR = new Color(0, 255, 0, 100);
    private final Color GRID_SELECTION_LASSO_COLOR = new Color(0, 255, 255, 100);
    
    
    private final Color[] GRID_PALETTE_INDEX_COLORS = {
        new Color(255, 255, 255),  // Blanc pour palette 0
        new Color(0, 255, 0),      // Vert pour palette 1
        new Color(255, 0, 255),    // Rose pour palette 2
        new Color(0, 0, 255)       // Bleu pour palette 3
    };



    public ImagePanel() {

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tileX = e.getX() / (ImageHandler.TILE_SIZE  * zoom);
                int tileY = e.getY() / (ImageHandler.TILE_SIZE  * zoom);

                if (e.getButton() == MouseEvent.BUTTON1) { // Left Click (Start Selection)
                    if (!e.isControlDown()) {
                        selectedTiles.clear(); // Reset selection if CTRL is not pressed
                    }
                    selectionStart = new Point(tileX, tileY);
                    selectionEnd = new Point(tileX, tileY);
                    repaint();
                } else if (e.getButton() == MouseEvent.BUTTON3) { // Right Click (Open Properties)
                    openTilePropertiesDialog();
                }
            }

        
            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionStart != null) {
                    int startX = Math.min(selectionStart.x, e.getX() / (ImageHandler.TILE_SIZE  * zoom));
                    int startY = Math.min(selectionStart.y, e.getY() / (ImageHandler.TILE_SIZE  * zoom));
                    int endX = Math.max(selectionStart.x, e.getX() / (ImageHandler.TILE_SIZE  * zoom));
                    int endY = Math.max(selectionStart.y, e.getY() / (ImageHandler.TILE_SIZE  * zoom));

                    for (int y = startY; y <= endY; y++) {
                        for (int x = startX; x <= endX; x++) {
                            selectedTiles.add(new Point(x, y));
                        }
                    }

                    selectionStart = null;
                    selectionEnd = null;
                    repaint();
                }
            }
        });



        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectionStart != null) {
                    selectionEnd = new Point(e.getX() / (ImageHandler.TILE_SIZE  * zoom), e.getY() / (ImageHandler.TILE_SIZE  * zoom));
                    repaint();  //update selecting rectangle
                }
            }
        });


        AppFileHandler.loadConfig(); 
        imageHandler = new ImageHandler();
        setBackground(Color.GRAY);
        setPreferredSize(new Dimension(800, 600));       

        setFocusable(true);
        requestFocusInWindow();
    }



    public void loadImage() 
    {
        BufferedImage loadedImage = AppFileHandler.selectInputImage(this);
        if (loadedImage != null) 
        {
            int returnCode = imageHandler.setImage(loadedImage);  
            
            if(returnCode ==0) 
            {
                setPreferredSize(new Dimension(imageHandler.getImage().getWidth() * zoom, imageHandler.getImage().getHeight() * zoom));
                revalidate();
                repaint();             
            }
            else
            {
                switch(returnCode)
                {
                    case ImageHandler.ERR_IMAGE_NOT_INDEXED->
                    this.messageHandler= new MessageHandler(returnCode,"Invalid Image : The image must be indexed (palette-based)", JOptionPane.ERROR_MESSAGE);
        
                    case ImageHandler.ERR_IMAGE_MORE_THAN_16_COLORS->
                    this.messageHandler= new MessageHandler(returnCode,"Invalid Image : The image uses more than the first 16 colors of the palette", JOptionPane.ERROR_MESSAGE);
        
                    case ImageHandler.ERR_IMAGE_NOT_8BPP->
                    this.messageHandler= new MessageHandler(returnCode,"Invalid Image : The image must be 8bpp", JOptionPane.ERROR_MESSAGE);
        
                    case ImageHandler.ERR_NO_IMAGE_NOR_MASK_LOADED_YET->
                    this.messageHandler= new MessageHandler(returnCode,"No image or mask loaded.", JOptionPane.ERROR_MESSAGE);  
        
                }                
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
            setPreferredSize(new Dimension(imageHandler.getImage() .getWidth() * zoom, imageHandler.getImage() .getHeight() * zoom));
            e.consume();
        }
        else
        {
            getParent().dispatchEvent(e);
            JScrollPane parent = (JScrollPane) getParent().getParent();
            JViewport viewport = parent.getViewport();
            Point viewPosition = viewport.getViewPosition();
    
            int scrollAmount = 16 * zoom;

            if (rotation < 0) {
                viewPosition.y = Math.max(viewPosition.y - scrollAmount, 0);
            } else {
                viewPosition.y = Math.min(viewPosition.y + scrollAmount, getHeight() - viewport.getHeight());
            }    
            viewport.setViewPosition(viewPosition);            
        }
    }




    private void drawTileOverlay(Graphics g) 
    {
        if (!assetsLoaded()) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(GRID_ARRAY_COLOR);

        int tileSize = ImageHandler.TILE_SIZE  * zoom;

        for (int y = 0; y < imageHandler.getMask().getHeight(); y++) 
        {
            for (int x = 0; x < imageHandler.getMask().getWidth(); x++) 
            {
                int paletteIndex = imageHandler.getMask().getTilePalette(x, y);
                int priority = imageHandler.getMask().getTilePriority(x, y);
                
                int drawX = x * tileSize;
                int drawY = y * tileSize;
                Tile tile = imageHandler.getMask().getTile(x, y);

                // Highlight if selected
                if (selectedTiles.contains(new Point(tile.getX(), tile.getY()))) {
                    g2d.setColor(GRID_SELECTED_TILE_COLOR);
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);
                }
                                
                if (priority == 1) {
                    g.setColor(GRID_HIGH_PRIORITY_ARRAY_COLOR);
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                    g2d.setColor(GRID_HIGH_PRIORITY_TILE_COLOR); // Brighter background
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);    
                }

                // draw palette index number with black outline
                String paletteText = String.valueOf(paletteIndex);
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(paletteText);
                int textHeight = fm.getHeight();
                
                int textX = drawX + (tileSize - textWidth) / 2;
                int textY = drawY + (tileSize + textHeight) / 2 - 3;

                g.setColor(Color.BLACK); //outline
                g.drawString(paletteText, textX - 1, textY);
                g.drawString(paletteText, textX + 1, textY);
                g.drawString(paletteText, textX, textY - 1);
                g.drawString(paletteText, textX, textY + 1);

                g.setColor(GRID_PALETTE_INDEX_COLORS[paletteIndex]);
                g.drawString(paletteText, textX, textY);
            }
        }
        
        // Draw selection rectangle
        if (selectionStart != null && selectionEnd != null) {
            int rectX = Math.min(selectionStart.x, selectionEnd.x) * tileSize;
            int rectY = Math.min(selectionStart.y, selectionEnd.y) * tileSize;
            int rectWidth = (Math.abs(selectionEnd.x - selectionStart.x) + 1) * tileSize;
            int rectHeight = (Math.abs(selectionEnd.y - selectionStart.y) + 1) * tileSize;

            g2d.setColor(GRID_SELECTION_LASSO_COLOR); // Cyan transparent
            g2d.fillRect(rectX, rectY, rectWidth, rectHeight);
            g2d.setColor(Color.CYAN); // Cyan border
            g2d.drawRect(rectX, rectY, rectWidth, rectHeight);
        }
    }

    private void openTilePropertiesDialog() 
    {
        if (!assetsLoaded()) return;
    
        // Get the first selected tile
        Point firstTile = selectedTiles.iterator().next();  
        Tile firstTileData = imageHandler.getMask().getTile(firstTile.x, firstTile.y);
    
        // Create the dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Tile Properties", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new GridLayout(3, 1));
    
        // Panel for Palette Selection
        JPanel palettePanel = new JPanel();
        palettePanel.setBorder(BorderFactory.createTitledBorder("Palette Index"));
        ButtonGroup paletteGroup = new ButtonGroup();
        JRadioButton[] paletteButtons = new JRadioButton[5];
    
        for (int i = 0; i < 5; i++) {
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
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imageHandler.getImage() != null) {
            g.drawImage(imageHandler.getImage() , 0, 0, imageHandler.getImage().getWidth() * zoom, imageHandler.getImage().getHeight() * zoom, null);
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
        if (zoom < 8.0) { // Limite à x8
            zoom += 1;
            revalidate();
            repaint();
        }
    }
    
    private void zoomOut() {
        if (zoom > 1.0) { // Min x1
            zoom -= 1;
            revalidate();
            repaint();
        }
    }

    public void handleKeyPress(KeyEvent e) 
    {
        if (!assetsLoaded()) return;   

        JScrollPane parent = (JScrollPane) getParent().getParent();
        JViewport viewport = parent.getViewport();
        Point viewPosition = viewport.getViewPosition();

        int scrollAmount = 16 * zoom;
        int keyCode = e.getKeyCode();

        switch (keyCode) 
        {
            case KeyEvent.VK_LEFT -> viewPosition.x = Math.max(viewPosition.x - scrollAmount, 0);
            case KeyEvent.VK_RIGHT -> viewPosition.x = Math.min(viewPosition.x + scrollAmount, getWidth() - viewport.getWidth());
            case KeyEvent.VK_UP -> viewPosition.y = Math.max(viewPosition.y - scrollAmount, 0);
            case KeyEvent.VK_DOWN -> viewPosition.y = Math.min(viewPosition.y + scrollAmount, getHeight() - viewport.getHeight());

            case KeyEvent.VK_H -> {
                // Met toutes les TILEs sélectionnées en priorité haute (1)
                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, imageHandler.getMask().getTilePalette(p.x, p.y), 1);
                }
            }

            case KeyEvent.VK_L -> {
                // Met toutes les TILEs sélectionnées en priorité basse (0)
                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, imageHandler.getMask().getTilePalette(p.x, p.y), 0);
                }
            }

            case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 0;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }  

            case KeyEvent.VK_1,  KeyEvent.VK_NUMPAD1 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 1;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }  
            
            case KeyEvent.VK_2,  KeyEvent.VK_NUMPAD2 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 2;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }              

            
            case KeyEvent.VK_3,  KeyEvent.VK_NUMPAD3 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 3;

                for (Point p : selectedTiles) {
                    imageHandler.getMask().setTileProperties(p.x, p.y, paletteIndex, imageHandler.getMask().getTilePriority(p.x, p.y));
                }
            }     

        }
        repaint();
        viewport.setViewPosition(viewPosition);
    }
}
