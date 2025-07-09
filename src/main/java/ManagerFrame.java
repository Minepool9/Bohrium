import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ManagerFrame {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private final List<File> files = new ArrayList<>();
    private Preferences prefs;
    private File downloadFolder;
    private JLabel iconLabel;
    private JLabel nameLabel;
    private JLabel idLabel;
    private JTextArea descArea;
    private DefaultListModel<String> depModel;

    public void createAndShowGUI() {
        prefs = Preferences.userNodeForPackage(ManagerFrame.class);
        String last = prefs.get("lastFolder", null);
        if (last != null) downloadFolder = new File(last);

        frame = new JFrame("Manage Installed Mods");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);

        model = new DefaultTableModel(new Object[] {"Disabled", "Mod File"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0;
            }
        };

        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setMinWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setMinWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.getColumnModel().getColumn(0).setHeaderRenderer(headerRenderer);

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                    int row = e.getFirstRow();
                    boolean disabled = (Boolean) model.getValueAt(row, 0);
                    File original = files.get(row);
                    String name = original.getName();
                    String base = name.endsWith(".disabled")
                        ? name.substring(0, name.length() - 9)
                        : name.substring(0, name.length() - 4);
                    Path source = original.toPath();
                    Path target = source.resolveSibling(base + (disabled ? ".disabled" : ".jar"));

                    new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() {
                            try {
                                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                                return true;
                            } catch (IOException ex) {
                                return false;
                            }
                        }
                        @Override
                        protected void done() {
                            try {
                                if (get()) {
                                    files.set(row, target.toFile());
                                } else {
                                    model.setValueAt(!disabled, row, 0);
                                    JOptionPane.showMessageDialog(frame,
                                        "Could not rename " + original.getName(),
                                        "Rename Failed",
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            } catch (Exception ex) {
                                model.setValueAt(!disabled, row, 0);
                                JOptionPane.showMessageDialog(frame,
                                    "Error: " + ex.getMessage(),
                                    "Rename Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }.execute();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(340, 0));

        JPanel detail = new JPanel();
        detail.setBorder(BorderFactory.createTitledBorder("Details"));
        detail.setLayout(new BorderLayout(5, 5));
        JPanel top = new JPanel(new BorderLayout(5, 5));
        iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(64, 64));
        top.add(iconLabel, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        nameLabel = new JLabel();
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 16f));
        idLabel = new JLabel();
        idLabel.setFont(idLabel.getFont().deriveFont(Font.PLAIN, 12f));
        text.add(nameLabel);
        text.add(idLabel);
        top.add(text, BorderLayout.CENTER);
        detail.add(top, BorderLayout.NORTH);

        descArea = new JTextArea();
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createTitledBorder("Description"));
        detail.add(descScroll, BorderLayout.CENTER);

        depModel = new DefaultListModel<>();
        JList<String> depList = new JList<>(depModel);
        JScrollPane depScroll = new JScrollPane(depList);
        depScroll.setBorder(BorderFactory.createTitledBorder("This mod requires:"));
        depScroll.setPreferredSize(new Dimension(0, 100));
        detail.add(depScroll, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, detail);
        frame.add(split);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int idx = table.getSelectedRow();
                    if (idx >= 0) loadDetails(files.get(idx));
                }
            }
        });

        frame.setVisible(true);
        loadFilesAsync();
    }

    private void loadDetails(File f) {
        try {
            ModParser.ModInfo mi = ModParser.parse(f);
            iconLabel.setIcon(mi.icon != null
                ? new ImageIcon(mi.icon.getScaledInstance(64, 64, Image.SCALE_SMOOTH))
                : null);
            nameLabel.setText(mi.name);
            idLabel.setText(mi.id);
            descArea.setText(mi.description);
            depModel.clear();
            depModel.addElement("Minecraft: " + mi.minecraftVersion);
            for (ModParser.Dependency d : mi.dependencies)
                depModel.addElement(d.modId
                    + (d.mandatory ? "" : " (optional)")
                    + " " + d.versionRange);
        } catch (IOException ex) {
            iconLabel.setIcon(null);
            nameLabel.setText("");
            idLabel.setText("");
            descArea.setText("Failed to load mod info.");
            depModel.clear();
        }
    }
	//AAAAAAAAAAAAAAA
	// dude why the FUCK do i have to do all of this
	// to ensure files can be disabled and enabled??? i hate myself
    private void loadFilesAsync() {
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (downloadFolder == null || !downloadFolder.isDirectory()) return null;
                File[] list = downloadFolder.listFiles((dir, name) ->
                    name.endsWith(".jar") || name.endsWith(".disabled"));
                if (list == null) return null;
                List<File> fileList = new ArrayList<>(List.of(list));
                fileList.sort((f1, f2) -> {
                    String n1 = f1.getName();
                    String n2 = f2.getName();
                    String b1 = n1.endsWith(".disabled")
                        ? n1.substring(0, n1.length() - 9)
                        : n1.substring(0, n1.length() - 4);
                    String b2 = n2.endsWith(".disabled")
                        ? n2.substring(0, n2.length() - 9)
                        : n2.substring(0, n2.length() - 4);
                    return b1.compareToIgnoreCase(b2);
                });
                for (File f : fileList) {
                    String n = f.getName();
                    boolean disabled = n.endsWith(".disabled");
                    String base = disabled
                        ? n.substring(0, n.length() - 9)
                        : n.substring(0, n.length() - 4);
                    publish(new Object[] {disabled, base, f});
                }
                return null;
            }
            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] c : chunks) {
                    model.addRow(new Object[] {c[0], c[1]});
                    files.add((File)c[2]);
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ManagerFrame().createAndShowGUI());
    }
}