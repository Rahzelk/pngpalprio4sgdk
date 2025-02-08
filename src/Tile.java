import java.io.Serializable;

public class Tile implements Serializable {
    private final int x, y;
    private int palette;
    private int priority;

    public Tile(int x, int y, int palette, int priority) {
        this.x = x;
        this.y = y;
        this.palette = palette;
        this.priority = priority;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPalette() {
        return palette;
    }

    public void setPalette(int palette) {
        this.palette = palette;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
