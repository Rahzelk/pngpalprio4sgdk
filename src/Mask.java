import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Mask  implements Serializable {
    private final int width;
    private final int height;
    private final List<Tile> tiles;

    public Mask(int width, int height) {
        this.width = width;
        this.height = height;
        tiles = new ArrayList<>();

        // Initialize the mask with default tiles
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles.add(new Tile(x, y, 0, 0)); // Default: palette 0, priority 0
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return tiles.get(y * width + x);
        }
        return null;
    }
    

    public int getTilePalette(int x, int y) {
        for (Tile tile : tiles) {
            if (tile.getX() == x && tile.getY() == y) {
                return tile.getPalette();
            }
        }
        return 0; // Valeur par défaut si la TILE n'existe pas
    }
    
    public int getTilePriority(int x, int y) {
        for (Tile tile : tiles) {
            if (tile.getX() == x && tile.getY() == y) {
                return tile.getPriority();
            }
        }
        return 0; // Valeur par défaut si la TILE n'existe pas
    }

    public List<Tile> getTiles() {
        return tiles;
    }
    public void setTileProperties(int x, int y, int palette, int priority) {
        Tile tile = getTile(x, y);
        if (tile != null) {
            tile.setPalette(palette);
            tile.setPriority(priority);
        }
    }
}
