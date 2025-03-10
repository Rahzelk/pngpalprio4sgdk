import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;



public class ImageHandler {

    public static final int SUCCESS = 0;
    public static final int ERR_IMAGE_NOT_INDEXED = 1;
    public static final int ERR_IMAGE_NOT_8BPP = 2;
    public static final int ERR_IMAGE_MORE_THAN_16_COLORS = 3;
    public static final int ERR_NO_IMAGE_NOR_MASK_LOADED_YET = 4;


    public static final int TILE_SIZE = 8;

    private BufferedImage image;
    private BufferedImage exportedImage;
    private Mask mask;



    public ImageHandler() {
        image = null;
        mask = null;
        exportedImage = null;
    }


    
    public static String getErrorMessage(int returnCode)
    {
        String msg = new String();

        switch(returnCode)
        {
            case ImageHandler.ERR_IMAGE_NOT_INDEXED :
                msg="Invalid Image : The image must be indexed (palette-based) ";
                break;

            case ImageHandler.ERR_IMAGE_MORE_THAN_16_COLORS:
                msg="Invalid Image : The image uses more than the first 16 colors of the palette ";
                break;

            case ImageHandler.ERR_IMAGE_NOT_8BPP:
                msg="Invalid Image : The image must be 8bpp ";
                break;

            case ImageHandler.ERR_NO_IMAGE_NOR_MASK_LOADED_YET:
                msg="No image or mask loaded.";
                break;
        } 
        return msg; 
    }



    public int setImage(BufferedImage loadedImage) 
    {        
        int returnCode = isValidIndexedImage(loadedImage);

        if (returnCode>0) {
            return returnCode;
        }

        image =loadedImage;
        int width = image.getWidth() / TILE_SIZE ;
        int height = image.getHeight() / TILE_SIZE ;
        mask = new Mask(width, height);
        
        return 0;
    }

    
    private int isValidIndexedImage(BufferedImage img) {
        // Vérifier que l'image a un IndexColorModel (palette)
        ColorModel cm = img.getColorModel();
        if (!(cm instanceof IndexColorModel)) {
            return ERR_IMAGE_NOT_INDEXED;
        }
    
        IndexColorModel icm = (IndexColorModel) cm;
    
        // check image in 8bpp
        if (icm.getPixelSize() != TILE_SIZE ) {
            return ERR_IMAGE_NOT_8BPP;
        }
    
        // check pixels index between 0 and 15
        Raster raster = img.getRaster();
        int width = img.getWidth();
        int height = img.getHeight();
    
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelIndex = raster.getSample(x, y, 0);
                if (pixelIndex < 0 || pixelIndex > 15) {
                    return ERR_IMAGE_MORE_THAN_16_COLORS;
                }
            }
        }
        return SUCCESS;
    }
            
    public int applyMask()
    {
        if (image == null || mask == null) {
            return ERR_NO_IMAGE_NOR_MASK_LOADED_YET;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Get original palette
        IndexColorModel originalColorModel = (IndexColorModel) image.getColorModel();
        
        int paletteSize = originalColorModel.getMapSize();
        byte[] reds = new byte[paletteSize];
        byte[] greens = new byte[paletteSize];
        byte[] blues = new byte[paletteSize];

        originalColorModel.getReds(reds);
        originalColorModel.getGreens(greens);
        originalColorModel.getBlues(blues);

        // Create new  palette 256 colors
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

        // Create new indexed image
        BufferedImage tempImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
        WritableRaster raster = tempImage.getRaster();

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
                            int originalPixel = image.getRaster().getSample(pixelX, pixelY, 0);
                            int newPixel = (originalPixel & 0x0F) | (paletteIndex << 4) | priorityBit;
                            raster.setSample(pixelX, pixelY, 0, newPixel);
                        }
                    }
                }
            }
        }

        // Apply new color model and save
        exportedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, newColorModel);
        exportedImage.setData(raster);
        
        //this.messageHandler= new MessageHandler(0,"Image exported successfully!.", JOptionPane.INFORMATION_MESSAGE);
        return SUCCESS;    
    }

    public void setMask(Mask mask) {
        this.mask = mask;
    }
    
    public Mask getMask() {
        return mask;
    }



    public BufferedImage getImage() {
        return image;
    }

    
    public BufferedImage getExportedImage() {
        return exportedImage;
    }


}
