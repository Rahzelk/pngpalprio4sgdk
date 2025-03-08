import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AppFileHandler {

    public static final int SUCCESS = 0;
    public static final int ERR_WRITE_EXPORT_IMAGE = 1;



    public static File lastImageDirectory = null;
    public static File lastMaskDirectory = null;


    private static final String CONFIG_FILE = "config.properties";




    public static void saveMask(Component panel, Mask mask) 
    {
        JFileChooser fileChooser = new JFileChooser(lastMaskDirectory);
        fileChooser.setDialogTitle("Save Mask");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Mask Files (*.msk)", "msk"));

        
        if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Ensure the file has the ".msk" extension
            if (!selectedFile.getName().toLowerCase().endsWith(".msk")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".msk");
            }                
            lastMaskDirectory = selectedFile.getParentFile(); 
            saveConfig();

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(selectedFile.getAbsolutePath()))) {
                oos.writeObject(mask);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }



    public static Mask loadMask(Component panel) {

        JFileChooser fileChooser = new JFileChooser(lastMaskDirectory);
        fileChooser.setDialogTitle("Load Mask");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Mask Files (*.msk)", "msk"));

        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            lastMaskDirectory = selectedFile.getParentFile();
            saveConfig();

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
                return (Mask) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public static Mask loadMask(String selectedFile) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(selectedFile))) {
            return (Mask) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    
    // Load last used directories
    public static void loadConfig() {
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

        // Save last used directories without overwriting other properties
        public static void saveConfig() {
            Properties properties = new Properties();

            // Charger les propriétés existantes
            try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                properties.load(fis);
            } catch (IOException ignored) {}

            // Mettre à jour les propriétés sans effacer les anciennes
            if (lastImageDirectory != null)
                properties.setProperty("lastImageDirectory", lastImageDirectory.getAbsolutePath());
            if (lastMaskDirectory != null)
                properties.setProperty("lastMaskDirectory", lastMaskDirectory.getAbsolutePath());

            // Sauvegarder le fichier sans écraser les autres configurations
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.store(fos, "Application Configuration");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    public static BufferedImage selectInputImage(Component panel)
    {
        JFileChooser fileChooser = new JFileChooser(lastImageDirectory);
        fileChooser.setDialogTitle("Load Image PNG");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Files (*.png)", "png"));

        if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) 
        {
            try
            {            
                File file = fileChooser.getSelectedFile();
                lastImageDirectory = file.getParentFile();
                saveConfig();        
                return ImageIO.read(file);
            }  catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    public static File selectOutputImage(Component panel) 
    {
        JFileChooser fileChooser = new JFileChooser(lastImageDirectory);
        fileChooser.setDialogTitle("Select output image");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
    
        if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) 
        {
            File outputFile = fileChooser.getSelectedFile();
            lastMaskDirectory = outputFile.getParentFile(); 
            saveConfig();

            if (!outputFile.getName().toLowerCase().endsWith(".png")) {
                return new File(outputFile.getAbsolutePath() + ".png");
            }
            
            return new File(outputFile.getAbsolutePath());
        }

        return null;
    }


    
    public static int writeOutputImage(BufferedImage outputImage, File outputFile)
    {
        try
        {
            ImageIO.write(outputImage, "png", outputFile);
        }
        catch (IOException e) {
            return ERR_WRITE_EXPORT_IMAGE;
        }        

        return SUCCESS;
    }



    public static void saveColorsToConfig(Color[] colors) {
        Properties config = new Properties();
        
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            config.load(input);
        } catch (IOException ignored) {}
    

        String[] keys = {
                "Array Border", "Selection Lasso", "Selected Tile", 
                "High Priority Border", "Palette Text",            
                "Palette 0", "Palette 1", "Palette 2", "Palette 3"
            };
    

        for (int i = 0; i < colors.length; i++) {
            config.setProperty(keys[i], ImagePanel.colorToHex(colors[i]));
        }
    
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            config.store(output, "Mask Colors Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }    


    public static  Color[]  loadColorsFromConfig() {
        Properties config = new Properties();
    
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            config.load(input);
        } catch (IOException ignored) {}
    


        String[] keys = {
            "Array Border", "Selection Lasso", "Selected Tile", 
            "High Priority Border", "Palette Text",            
            "Palette 0", "Palette 1", "Palette 2", "Palette 3"
        };
    
        
        Color[] defaultColors = ImagePanel.getDefaultColors();
    
        for (int i = 0; i < keys.length; i++) {
            String hex = config.getProperty(keys[i]);
            if (hex != null) {
                defaultColors[i] = ImagePanel.hexToColor(hex);
            }
        }

        return defaultColors;
    }
}
