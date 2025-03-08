import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PngPalettePriorityEditor extends JFrame {
    private ImagePanel imagePanel;

    private final JMenu imageMenu;
    private final JMenu maskMenu;
    private final JMenu optionsMenu;
    private final JMenu viewMenu;
    

    private boolean showGrid = true;
    private boolean showPaletteIndex = true;
    private boolean viewPaletteZero = false;
    
    
    public void allowMenuChoice()
    {
        maskMenu.setEnabled(true);
        optionsMenu.setEnabled(true);
        viewMenu.setEnabled(true);

        JMenuItem exportImage = new JMenuItem("Export Image");
        exportImage.addActionListener(e -> imagePanel.exportImage());
        imageMenu.add(exportImage);
    }

    public PngPalettePriorityEditor()
    {
        setTitle("PNG Palette and Priority Editor for SGDK");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize image panel
        imagePanel = new ImagePanel(this);
        imagePanel.addMouseWheelListener(imagePanel);
        
        JScrollPane scrollPane = new JScrollPane(imagePanel, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        add(scrollPane);

        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            private final Timer timer = new Timer(25, e -> imagePanel.doScrollbarUpdate()); // Attendre 200ms avant repaint

            @Override
            public void stateChanged(ChangeEvent e) {
                timer.restart(); // Redémarre le timer à chaque mouvement
            }
        });

        // Create menu
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);


        // Menu "Image"
        imageMenu = new JMenu("Image");
        menuBar.add(imageMenu);

        JMenuItem loadImage = new JMenuItem("Load Image");
        loadImage.addActionListener(e -> imagePanel.loadImage());
        imageMenu.add(loadImage);

        

        // Menu "Mask"
        maskMenu = new JMenu("Mask");
        menuBar.add(maskMenu);
        maskMenu.setEnabled(false);
        JMenuItem loadMask = new JMenuItem("Load Mask");
        maskMenu.add(loadMask);
        loadMask.addActionListener(e -> imagePanel.loadMask());

        JMenuItem saveMask = new JMenuItem("Save Mask");
        maskMenu.add(saveMask);
        saveMask.addActionListener(e -> imagePanel.saveMask());


        // Menu options
        optionsMenu = new JMenu("Options");
        optionsMenu.setEnabled(false);
        menuBar.add(optionsMenu);
        JMenuItem maskColorsItem = new JMenuItem("Mask Colors...");
        maskColorsItem.addActionListener(e -> imagePanel.openMaskColorSettings());
        
        optionsMenu.add(maskColorsItem);

        

        viewMenu = new JMenu("View");
        viewMenu.setEnabled(false);
        // Option "View Grid"
        JCheckBoxMenuItem viewGridItem = new JCheckBoxMenuItem("View Grid", showGrid);
        viewGridItem.addActionListener(e -> {
            showGrid = viewGridItem.isSelected();
            imagePanel.repaint();
        });

        // Option "View Palette Index Number"
        JCheckBoxMenuItem viewPaletteItem = new JCheckBoxMenuItem("View Palette Index", showPaletteIndex);
        viewPaletteItem.addActionListener(e -> {
            showPaletteIndex = viewPaletteItem.isSelected();
            imagePanel.repaint();
        });

        // Option "View Palette Index Number"
        JCheckBoxMenuItem viewPaletteZeroItem = new JCheckBoxMenuItem("View Palette 0", viewPaletteZero);
        viewPaletteZeroItem.addActionListener(e -> {
            viewPaletteZero = viewPaletteZeroItem.isSelected();
            imagePanel.repaint();
        });

        // Ajouter les options au menu "View"
        viewMenu.add(viewGridItem);
        viewMenu.add(viewPaletteItem);
        viewMenu.add(viewPaletteZeroItem);


        // Ajouter le menu à la barre de menu
        menuBar.add(viewMenu);

    
        // Menu "Help"
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem instructions = new JMenuItem("Instructions");
        helpMenu.add(instructions);
        instructions.addActionListener(e -> showInstructions());

        setupStatusBar();
    }


        // Barre d'état
    private JLabel statusBar;

    private void setupStatusBar() {
        statusBar = new JLabel("You should first load an image...");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        getContentPane().add(statusBar, BorderLayout.SOUTH);
    }

    // Mettre à jour le texte de la barre d'état
    public void updateStatusBar(String text) {
        statusBar.setText(text);
    }


    private void showInstructions() 
    {
        String instructionsText = """
        - - - - - - - - HOW TO USE IT ? - - - - - - - - - - - - - - - - 

        1] Load an image
            Note : the image must be an indexed PNG (8bpp / only first 16 colors used)
            
        2] Edit the priority and palette index via the grid mask and hotkeys (H/L and 0-4 keys) 

        3] Save your mask (if you like to reuse it next time, it's not binded to the image file)

        4] Export Image : it will apply the mask pal & priority informations to the loaded image andsave it as a new PNG file.


        - - - - - - - CONTROLS & SHORTCUTS - - - - - - - - - - - - - - 
        
        * Zoom & Navigation:
          - SHIFT + Mouse Wheel: Zoom in/out
          - Arrow Keys: Move the view
        
        * Selection:
          - Left Click & Drag: Select multiple tiles (lasso selection)
          - CTRL + Left Click: Add multiple selection areas

        * Editing Tiles:
          You can right-click on tiles to edit their properties,
          But you may prefer directly use these hotkeys :
           - H: Set selected tiles to Priority 1 (High)
           - L: Set selected tiles to Priority 0 (Low)
           - 0, 1, 2, 3: Change the palette index of selected tiles
           
           - CTRL+Z : undo last priority or palette change
           

        - - - - - - - BATCH MODE - - - - - - - - - - - - - - - - - - - 

        Run the editor from the command line:
           java -jar PPPE4SGDK.jar --b <image_path> <mask_path> <export_path>

        The image must be an indexed PNG (8bpp / only first 16 colors used)
        The mask file must be a valid .msk file, made previously using the GUI
        

        Enjoy !
        Rahzelk          
        """;

        JTextArea textArea = new JTextArea(instructionsText);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
    
        
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) 
    {
        if (args.length == 4 && args[0].equals("--b")) 
        {
            String imagePath = args[1];
            String maskPath = args[2];
            String exportPath = args[3];


            System.out.println("Batch mode detected. Validating inputs...");

            // Vérification des fichiers
            File imageFile = new File(imagePath);
            File maskFile = new File(maskPath);

            if (!imageFile.exists() || !imageFile.isFile()) {
                System.err.println("Error: Image file not found: " + imagePath);
                System.exit(1);
            }

            if (!maskFile.exists() || !maskFile.isFile()) {
                System.err.println("Error: Mask file not found: " + maskPath);
                System.exit(1);
            }


            System.out.println("Batch mode detected. Processing...");
            try 
            {
                ImageHandler imageHandler = new ImageHandler();
                int returnCode = imageHandler.setImage(ImageIO.read(new File(imagePath)));
                if(returnCode > 0){
                    System.out.println(ImageHandler.getErrorMessage(returnCode));
                    return;
                }  

                Mask mask = AppFileHandler.loadMask(maskPath); // Charge le mask
                imageHandler.setMask(mask);
                imageHandler.applyMask();
                
                if(imageHandler.getExportedImage()!=null){
                    if(AppFileHandler.writeOutputImage(imageHandler.getExportedImage(), new File(exportPath))==AppFileHandler.ERR_WRITE_EXPORT_IMAGE)
                    {
                        System.out.println("Error while writing the export file !");
                        return;
                    }
                    else
                    {
                        System.out.println("Export successful: " + exportPath);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error in batch mode: " + e.getMessage());
                e.printStackTrace();
            }
            System.exit(0);
        } else {
            SwingUtilities.invokeLater(() -> new PngPalettePriorityEditor().setVisible(true));
        }
    }

    public boolean showGrid() {
        return showGrid;
    }

    public boolean showPaletteIndex() {
        return showPaletteIndex;
    }

    public boolean viewPaletteZero() {
        return viewPaletteZero;
    }
}
