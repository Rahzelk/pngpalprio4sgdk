import java.io.*;

public class FileHandler {
    public static void saveMask(Mask mask, String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(mask);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Mask loadMask(String filePath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            return (Mask) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
