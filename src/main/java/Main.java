//Main.java
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        ImageIO.scanForPlugins();
        SwingUtilities.invokeLater(() -> new MainFrame().createAndShowGUI());
    }
}