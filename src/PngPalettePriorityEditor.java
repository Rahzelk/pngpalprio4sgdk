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
        --- Controls & Shortcuts ---
        
        Selection:
        - Left Click & Drag: Select multiple tiles (lasso selection)
        - CTRL + Click: Add multiple selections
        
        Editing Tiles:
        - Right Click: Open properties menu to change palette/priority
        - H: Set selected tiles to Priority 1 (High)
        - L: Set selected tiles to Priority 0 (Low)
        - 0, 1, 2, 3: Change the palette of selected tiles
        
        Zoom & Navigation:
        - SHIFT + Mouse Wheel: Zoom in/out
        - Arrow Keys: Move the view
        - Scrollbars: Scroll horizontally and vertically
        
        Mask Operations:
        - Load Mask: Load a mask file
        - Save Mask: Save the current mask
        
        Image Operations:
        - Load Image: Open an image file
        - Export Image: Save the modified image with mask effects
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
            imageHandler.setImage(ImageIO.read(new File(imagePath)));

            Mask mask = AppFileHandler.loadMask(maskPath); // Charge le mask
            imageHandler.setMask(mask);
            imageHandler.applyMask();
            AppFileHandler.writeOutputImage(imageHandler.getExportedImage(), new File(exportPath));
            System.out.println("Export successful: " + exportPath);

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
