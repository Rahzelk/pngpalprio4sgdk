import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ImagePanel extends JPanel implements MouseWheelListener  {
    private BufferedImage originalImage;
    private Mask mask;
    private int zoom = 1;
    private Set<Point> selectedTiles = new HashSet<>();
    private Point selectionStart = null;
    private Point selectionEnd = null;
    private File lastImageDirectory = null;
    private File lastMaskDirectory = null;


    private static final String CONFIG_FILE = "config.properties";

    private final int TILE_SIZE = 8;
    private final Color GRID_COLOR = new Color(192, 192, 192);
    private final Color PRIORITY_COLOR = new Color(0, 0, 255);
    private final Color[] PALETTE_COLORS = {
        new Color(255, 255, 255),  // Blanc pour palette 0
        new Color(0, 255, 0),      // Vert pour palette 1
        new Color(255, 0, 255),    // Rose pour palette 2
        new Color(0, 0, 255)       // Bleu pour palette 3
    };



    // Load last used directories
    private void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            String imagePath = properties.getProperty("lastImageDirectory");
            String maskPath = properties.getProperty("lastMaskDirectory");

            if (imagePath != null) lastImageDirectory = new File(imagePath);
            if (maskPath != null) lastMaskDirectory = new File(maskPath);

        } catch (IOException e) {
            System.out.println("No previous config found. Defaulting to null.");
        }
    }   

    // Save last used directories
    private void saveConfig() {
        Properties properties = new Properties();
        if (lastImageDirectory != null)
            properties.setProperty("lastImageDirectory", lastImageDirectory.getAbsolutePath());
        if (lastMaskDirectory != null)
            properties.setProperty("lastMaskDirectory", lastMaskDirectory.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Last Used Directories");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    

    public ImagePanel() {
        setBackground(Color.GRAY);
        setPreferredSize(new Dimension(800, 600));
            
        loadConfig(); 

        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tileX = e.getX() / (TILE_SIZE  * zoom);
                int tileY = e.getY() / (TILE_SIZE  * zoom);

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
                    int startX = Math.min(selectionStart.x, e.getX() / (TILE_SIZE  * zoom));
                    int startY = Math.min(selectionStart.y, e.getY() / (TILE_SIZE  * zoom));
                    int endX = Math.max(selectionStart.x, e.getX() / (TILE_SIZE  * zoom));
                    int endY = Math.max(selectionStart.y, e.getY() / (TILE_SIZE  * zoom));

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
                    selectionEnd = new Point(e.getX() / (TILE_SIZE  * zoom), e.getY() / (TILE_SIZE  * zoom));
                    repaint();  // Mise à jour pour dessiner le rectangle
                }
            }
        });

        setFocusable(true);
        requestFocusInWindow();
    }

    public void loadImage() {
        JFileChooser fileChooser = new JFileChooser(lastImageDirectory);
        fileChooser.setDialogTitle("Load Image PNG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Files (*.png)", "png"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastImageDirectory = file.getParentFile();
            saveConfig();

            try {
                BufferedImage loadedImage = ImageIO.read(file);

                if (loadedImage != null) {

                    if (!isValidIndexedImage(loadedImage)) {
                        return; // Annule le chargement si l'image est invalide
                    }
                    originalImage =loadedImage;
                    int width = originalImage.getWidth() / TILE_SIZE ;
                    int height = originalImage.getHeight() / TILE_SIZE ;
                    mask = new Mask(width, height);
                    setPreferredSize(new Dimension(originalImage.getWidth() * zoom, originalImage.getHeight() * zoom));
                    revalidate();
                    repaint();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    

    private boolean isValidIndexedImage(BufferedImage img) {
        // Vérifier que l'image a un IndexColorModel (palette)
        ColorModel cm = img.getColorModel();
        if (!(cm instanceof IndexColorModel)) {
            JOptionPane.showMessageDialog(this, "Error: The image must be indexed (palette-based).", "Invalid Image", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    
        IndexColorModel icm = (IndexColorModel) cm;
    
        // check image in 8bpp
        if (icm.getPixelSize() != TILE_SIZE ) {
            JOptionPane.showMessageDialog(this, "Error: The image must be 8bpp.", "Invalid Image", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    
        // check pixels index between 0 and 15
        Raster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = raster.getSample(x, y, 0);
                if (pixelIndex < 0 || pixelIndex > 15) {
                    JOptionPane.showMessageDialog(this, "Error: The image uses more than the first 16 colors of the palette. ", "Invalid Image", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }
    
        return true;
    }
            
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

        if(originalImage == null) return;

        int rotation = e.getWheelRotation();

        if (e.isShiftDown()) 
        { // Vérifie si SHIFT est enfoncé

            if (rotation < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
            setPreferredSize(new Dimension(originalImage.getWidth() * zoom, originalImage.getHeight() * zoom));
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




    private void drawTileOverlay(Graphics g) {
        if (mask == null || originalImage == null) return;


        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(new Color(200, 200, 200, 150)); // Light gray for grid

        int tileSize = TILE_SIZE  * zoom;

        for (int y = 0; y < mask.getHeight(); y++) 
        {
            for (int x = 0; x < mask.getWidth(); x++) 
            {
                int paletteIndex = mask.getTilePalette(x, y);
                int priority = mask.getTilePriority(x, y);
                
                int drawX = x * tileSize;
                int drawY = y * tileSize;
                Tile tile = mask.getTile(x, y);

                // Highlight if selected
                if (selectedTiles.contains(new Point(tile.getX(), tile.getY()))) {
                    g2d.setColor(new Color(0, 255, 0, 100)); // Light green selection
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);
                }
                                
                if (priority == 1) {
                    g.setColor(PRIORITY_COLOR);
                    g.drawRect(drawX, drawY, tileSize, tileSize);
                    g2d.setColor(new Color(255, 255, 255, 50)); // Brighter
                    g2d.fillRect(drawX, drawY, tileSize, tileSize);    
                }

                // Affichage de l'index de palette avec contour noir
                String paletteText = String.valueOf(paletteIndex);
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(paletteText);
                int textHeight = fm.getHeight();
                
                int textX = drawX + (tileSize - textWidth) / 2;
                int textY = drawY + (tileSize + textHeight) / 2 - 3;

                g.setColor(Color.BLACK);
                g.drawString(paletteText, textX - 1, textY);
                g.drawString(paletteText, textX + 1, textY);
                g.drawString(paletteText, textX, textY - 1);
                g.drawString(paletteText, textX, textY + 1);

                g.setColor(PALETTE_COLORS[paletteIndex]);
                g.drawString(paletteText, textX, textY);
            }
        }
        
        // Draw selection rectangle
        if (selectionStart != null && selectionEnd != null) {
            int rectX = Math.min(selectionStart.x, selectionEnd.x) * tileSize;
            int rectY = Math.min(selectionStart.y, selectionEnd.y) * tileSize;
            int rectWidth = (Math.abs(selectionEnd.x - selectionStart.x) + 1) * tileSize;
            int rectHeight = (Math.abs(selectionEnd.y - selectionStart.y) + 1) * tileSize;

            g2d.setColor(new Color(0, 255, 255, 100)); // Cyan transparent
            g2d.fillRect(rectX, rectY, rectWidth, rectHeight);
            g2d.setColor(Color.CYAN); // Cyan border
            g2d.drawRect(rectX, rectY, rectWidth, rectHeight);
        }
    }

    private void openTilePropertiesDialog() {
        if (mask == null || selectedTiles.isEmpty()) {
            return;
        }
    
        // Get the first selected tile
        Point firstTile = selectedTiles.iterator().next();  
        Tile firstTileData = mask.getTile(firstTile.x, firstTile.y);
    
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
                mask.setTileProperties(p.x, p.y, newPalette, newPriority);
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
        if (originalImage != null) {
            g.drawImage(originalImage, 0, 0, originalImage.getWidth() * zoom, originalImage.getHeight() * zoom, null);
        }
        drawTileOverlay(g);
    }

    public void exportImage() {
        if (originalImage == null || mask == null) {
            JOptionPane.showMessageDialog(null, "No image or mask loaded.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        JFileChooser fileChooser = new JFileChooser(lastImageDirectory);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
    
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            try {
                File outputFile = fileChooser.getSelectedFile();
                lastMaskDirectory = outputFile.getParentFile(); 
                saveConfig();

                if (!outputFile.getName().toLowerCase().endsWith(".png")) {
                    outputFile = new File(outputFile.getAbsolutePath() + ".png");
                }
    
                int width = originalImage.getWidth();
                int height = originalImage.getHeight();
    
                // Create new indexed image
                BufferedImage exportedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
    
                // Get original palette
                IndexColorModel originalColorModel = (IndexColorModel) originalImage.getColorModel();
                
                                // Récupération de la palette d'origine
                int paletteSize = originalColorModel.getMapSize();
                byte[] reds = new byte[paletteSize];
                byte[] greens = new byte[paletteSize];
                byte[] blues = new byte[paletteSize];

                originalColorModel.getReds(reds);
                originalColorModel.getGreens(greens);
                originalColorModel.getBlues(blues);

                // Création d'une palette de 256 couleurs
                byte[] newReds = new byte[256];
                byte[] newGreens = new byte[256];
                byte[] newBlues = new byte[256];

                // Duplication des 16 premières couleurs sur 16 palettes
                for (int i = 0; i < 16; i++) {
                    for (int j = 0; j < 16; j++) {
                        int newIndex = (i * 16) + j; // Génère un index de 0 à 255
                        int originalIndex = j % paletteSize; // Récupère l'index original sans dépasser
                        newReds[newIndex] = reds[originalIndex];
                        newGreens[newIndex] = greens[originalIndex];
                        newBlues[newIndex] = blues[originalIndex];
                    }
                }

                // Création d'un nouvel IndexColorModel avec la palette corrigée
                IndexColorModel newColorModel = new IndexColorModel(8, 256, newReds, newGreens, newBlues);

                WritableRaster raster = exportedImage.getRaster();
    
                // Apply mask modifications
                for (int tileY = 0; tileY < mask.getHeight(); tileY++) {
                    for (int tileX = 0; tileX < mask.getWidth(); tileX++) {
                        Tile tile = mask.getTile(tileX, tileY);
                        int paletteIndex = tile.getPalette();
                        int priorityBit = tile.getPriority() << 7;
    
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                int pixelX = tileX * 8 + x;
                                int pixelY = tileY * 8 + y;
    
                                if (pixelX < width && pixelY < height) {
                                    int originalPixel = originalImage.getRaster().getSample(pixelX, pixelY, 0);
                                    int newPixel = (originalPixel & 0x0F) | (paletteIndex << 4) | priorityBit;
                                    raster.setSample(pixelX, pixelY, 0, newPixel);
                                }
                            }
                        }
                    }
                }
    
                // Apply new color model and save
                BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, newColorModel);
                finalImage.setData(raster);
                ImageIO.write(finalImage, "png", outputFile);
                JOptionPane.showMessageDialog(null, "Image exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Failed to export image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    


    public void saveMask() 
    {
        if(mask == null)
        {
            JOptionPane.showMessageDialog(null, "No image or mask loaded.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            JFileChooser fileChooser = new JFileChooser(lastMaskDirectory);
            fileChooser.setDialogTitle("Save Mask");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Mask Files (*.msk)", "msk"));

            
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                
                // Ensure the file has the ".msk" extension
                if (!selectedFile.getName().toLowerCase().endsWith(".msk")) {
                    selectedFile = new File(selectedFile.getAbsolutePath() + ".msk");
                }                
                lastMaskDirectory = selectedFile.getParentFile(); 
                saveConfig();
                FileHandler.saveMask(mask, selectedFile.getAbsolutePath());
            }
        }
    }

    public void loadMask() {
        JFileChooser fileChooser = new JFileChooser(lastMaskDirectory);
        fileChooser.setDialogTitle("Load Mask");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Mask Files (*.msk)", "msk"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            lastMaskDirectory = selectedFile.getParentFile();
            saveConfig();
            mask = FileHandler.loadMask(selectedFile.getAbsolutePath());
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

    public void setMask(Mask mask) {
        this.mask = mask;
        repaint();
    }

    public void handleKeyPress(KeyEvent e) {
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
                    mask.setTileProperties(p.x, p.y, mask.getTilePalette(p.x, p.y), 1);
                }
            }

            case KeyEvent.VK_L -> {
                // Met toutes les TILEs sélectionnées en priorité basse (0)
                for (Point p : selectedTiles) {
                    mask.setTileProperties(p.x, p.y, mask.getTilePalette(p.x, p.y), 0);
                }
            }

            case KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 0;

                for (Point p : selectedTiles) {
                    mask.setTileProperties(p.x, p.y, paletteIndex, mask.getTilePriority(p.x, p.y));
                }
            }  

            case KeyEvent.VK_1,  KeyEvent.VK_NUMPAD1 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 1;

                for (Point p : selectedTiles) {
                    mask.setTileProperties(p.x, p.y, paletteIndex, mask.getTilePriority(p.x, p.y));
                }
            }  
            
            case KeyEvent.VK_2,  KeyEvent.VK_NUMPAD2 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 2;

                for (Point p : selectedTiles) {
                    mask.setTileProperties(p.x, p.y, paletteIndex, mask.getTilePriority(p.x, p.y));
                }
            }              

            
            case KeyEvent.VK_3,  KeyEvent.VK_NUMPAD3 -> {
                // Modifie la palette de toutes les TILEs sélectionnées (0 à 3)
                int paletteIndex = 3;

                for (Point p : selectedTiles) {
                    mask.setTileProperties(p.x, p.y, paletteIndex, mask.getTilePriority(p.x, p.y));
                }
            }     

        }
        repaint();
        viewport.setViewPosition(viewPosition);
    }
}
