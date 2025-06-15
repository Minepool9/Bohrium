// ConfirmFrame.java
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;

public class ConfirmFrame extends JFrame {
    private java.util.List<ModEntry> mods;
    private JPanel listPanel;
    private Set<ModEntry> selectedMods;
    private Map<Integer, Set<ModEntry>> pageSelections;
    private File downloadFolder;
    private JFrame mainFrame;
    private String currentMC;
    private String currentLoader;

    public ConfirmFrame(java.util.List<ModEntry> initial, Set<ModEntry> selectedMods, 
                       Map<Integer, Set<ModEntry>> pageSelections, File downloadFolder, 
                       JFrame mainFrame, String currentMC, String currentLoader) {
        super("Confirm Downloads");
        this.mods = new ArrayList<>(initial);
        this.selectedMods = selectedMods;
        this.pageSelections = pageSelections;
        this.downloadFolder = downloadFolder;
        this.mainFrame = mainFrame;
        this.currentMC = currentMC;
        this.currentLoader = currentLoader;
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        JLabel label = new JLabel("The following mod(s) will be downloaded:");
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(label, BorderLayout.NORTH);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setPreferredSize(new Dimension(400, 200));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JButton back = new JButton("Back");
        back.addActionListener(e -> dispose());
        JButton cont = new JButton("Continue");
        cont.addActionListener(e -> {
            if (!mods.isEmpty()) {
                DownloadFrame df = new DownloadFrame(mainFrame);
                new DownloadWorker(mods, df, downloadFolder, mainFrame, currentMC, currentLoader).execute();
                df.setVisible(true);
            }
            dispose();
        });
        buttons.add(back);
        buttons.add(cont);
        add(buttons, BorderLayout.SOUTH);

        setSize(400, 300);
        setLocationRelativeTo(mainFrame);
        refreshList();
    }

    private void refreshList() {
        listPanel.removeAll();
        for (ModEntry m : mods) {
            JPanel row = new JPanel(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            row.setBackground(Color.WHITE);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(3, 5, 3, 5)
            ));
            
            JLabel titleLabel = new JLabel(m.title);
            titleLabel.setOpaque(false);
            row.add(titleLabel, BorderLayout.CENTER);
            
            JButton remove = new JButton("✕");
            remove.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(150, 0, 0)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
            remove.setBackground(Color.RED);
            remove.setForeground(Color.WHITE);
            remove.setFont(remove.getFont().deriveFont(Font.BOLD));
            remove.setPreferredSize(new Dimension(30, 30));
            remove.setMaximumSize(new Dimension(30, 30));
            remove.setMinimumSize(new Dimension(30, 30));
            remove.setOpaque(true);
            remove.setContentAreaFilled(true);
            remove.setFocusPainted(false);
            
            remove.addActionListener(e -> {
                mods.remove(m);
                selectedMods.remove(m);
                for (Set<ModEntry> selections : pageSelections.values()) {
                    selections.remove(m);
                }
                refreshList();
                listPanel.revalidate();
                listPanel.repaint();
            });
            row.add(remove, BorderLayout.EAST);
            listPanel.add(row);
            listPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }
}