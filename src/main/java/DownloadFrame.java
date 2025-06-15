// DownloadFrame.java
import javax.swing.*;
import java.awt.*;

public class DownloadFrame extends JFrame {
    private final JProgressBar bar;
    private final JTextArea area;
    
    public DownloadFrame(JFrame parent) {
        super("Downloading Mods");
        setLayout(new BorderLayout());
        bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        add(bar, BorderLayout.NORTH);
        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }
    
    public void updateProgressBytes(long done, long total) {
        int pct = total > 0 ? (int)(100 * done / total) : 0;
        SwingUtilities.invokeLater(() -> bar.setValue(pct));
    }
    
    public void log(String msg) {
        SwingUtilities.invokeLater(() -> area.append(msg));
    }
}