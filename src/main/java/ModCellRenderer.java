// ModCellRenderer.java
import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class ModCellRenderer implements ListCellRenderer<ModEntry> {
    private final Set<ModEntry> selectedMods;
    
    public ModCellRenderer(Set<ModEntry> selectedMods) {
        this.selectedMods = selectedMods;
    }
    
    public Component getListCellRendererComponent(JList<? extends ModEntry> list, ModEntry value, int idx, boolean sel, boolean foc) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(value.icon != null ? value.icon : UIManager.getIcon("OptionPane.warningIcon"));
        p.add(iconLabel, BorderLayout.WEST);
        p.add(new JLabel(value.title), BorderLayout.CENTER);
        p.add(new JLabel(value.slug, SwingConstants.RIGHT), BorderLayout.EAST);
        
        if (selectedMods.contains(value)) {
            p.setBackground(new Color(200, 220, 255));
        } else {
            p.setBackground(list.getBackground());
        }
        p.setOpaque(true);
        return p;
    }
}