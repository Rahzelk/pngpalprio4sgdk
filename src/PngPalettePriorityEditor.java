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

        loadMask.addActionListener(e -> imagePanel.loadMask());
        saveMask.addActionListener(e -> imagePanel.saveMask());

        maskMenu.add(loadMask);
        maskMenu.add(saveMask);
        menuBar.add(maskMenu);

        setJMenuBar(menuBar);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PngPalettePriorityEditor().setVisible(true));
    }
}
