import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;

public class PngPalettePriorityEditor extends JFrame {
    private ImagePanel imagePanel;

    public PngPalettePriorityEditor() {
        setTitle("PNG Palette and Priority Editor for SGDK");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize image panel
        imagePanel = new ImagePanel();
        imagePanel.addMouseWheelListener(imagePanel);
        
        JScrollPane scrollPane = new JScrollPane(imagePanel, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane);

        // Create menu
        JMenuBar menuBar = new JMenuBar();

        // Menu "Image"
        JMenu imageMenu = new JMenu("Image");
        JMenuItem loadImage = new JMenuItem("Load Image");
        JMenuItem exportImage = new JMenuItem("Export Image");

        loadImage.addActionListener(e -> imagePanel.loadImage());
        exportImage.addActionListener(e -> imagePanel.exportImage());

        imageMenu.add(loadImage);
        imageMenu.add(exportImage);
        menuBar.add(imageMenu);

        // Menu "Mask"
        JMenu maskMenu = new JMenu("Mask");
        JMenuItem loadMask = new JMenuItem("Load Mask");
        JMenuItem saveMask = new JMenuItem("Save Mask");

        // Menu "Help"
        JMenu helpMenu = new JMenu("Help");
        JMenuItem instructions = new JMenuItem("Instructions");
        helpMenu.add(instructions);
        loadMask.addActionListener(e -> imagePanel.loadMask());
        saveMask.addActionListener(e -> imagePanel.saveMask());

        instructions.addActionListener(e -> showInstructions());
        
        maskMenu.add(loadMask);
        maskMenu.add(saveMask);
        menuBar.add(maskMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }




    private void showInstructions() {
        String instructionsText = """
        - - - - - - - - HOW TO USE IT ? - - - - - - - - - - - - - - - - 

        1] Load an image
            Note : the image must be an indexed PNG (8bpp / only first 16 colors used)
            
        2] Edit the priority and palette index via the grid mask and hotkeys (H/L and 0-4 keys) 

        3] Save your mask (if you like to reuse it next time, it's not binded to the image file)

        4] Export Image : it will apply the mask pal & priority informations to the loaded image


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


        - - - - - - - BATCH MODE - - - - - - - - - - - - - - - - - - - 

        Run the editor from the command line:
           java -jar PPPE4SGDK.jar --b <image_path> <mask_path> <export_path>

        The image must be an indexed PNG (8bpp / only first 16 colors used)
        The mask file must be a valid .msk file, made previously using the GUI
            
        ===========================================            
        """;

        JTextArea textArea = new JTextArea(instructionsText);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Instructions", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
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
                AppFileHandler.writeOutputImage(imageHandler.getExportedImage(), new File(exportPath));
                System.out.println("Export successful: " + exportPath);
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
}
