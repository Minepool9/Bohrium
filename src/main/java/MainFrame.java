// MainFrame.java
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;

public class MainFrame {
	JFrame frame;
	JTextField pageField;
	JButton leftBtn, rightBtn;
	JComboBox<Integer> perPageCombo;
	DefaultListModel<ModEntry> listModel;
	File downloadFolder;
	JButton fetchBtn, downloadBtn;
	int currentPage = 1, perPage = 20, maxPages = Integer.MAX_VALUE;
	ExecutorService iconExecutor;
	JLabel folderStatus;
	Preferences prefs;
	String currentMC = "1.21.4";
	String currentLoader = "fabric";
	Set<ModEntry> selectedMods = new HashSet<>();
	Map<Integer, Set<ModEntry>> pageSelections = new HashMap<>();
	JList<ModEntry> modList;

	public void createAndShowGUI() {
		prefs = Preferences.userNodeForPackage(MainFrame.class);
		iconExecutor = Executors.newFixedThreadPool(10);

		frame = new JFrame("Bohrium");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 500);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				iconExecutor.shutdownNow();
			}
		});
		frame.setLocationRelativeTo(null);

		JMenuBar menuBar = new JMenuBar();
		JMenu settingsMenu = new JMenu("Settings and versions");
		settingsMenu.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				showSettingsDialog();
			}
		});
		menuBar.add(settingsMenu);
		frame.setJMenuBar(menuBar);

		JPanel mainPanel = new JPanel(new BorderLayout());

		fetchBtn = new JButton("Fetch Mods");
		fetchBtn.addActionListener(e -> fetchMods());
		JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		top.add(fetchBtn);
		mainPanel.add(top, BorderLayout.NORTH);

		listModel = new DefaultListModel<>();
		modList = new JList<>(listModel);
		modList.setCellRenderer(new ModCellRenderer(selectedMods));
		modList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		modList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int index = modList.locationToIndex(e.getPoint());
				if (index >= 0) {
					ModEntry mod = listModel.getElementAt(index);
					if (selectedMods.contains(mod)) {
						selectedMods.remove(mod);
					} else {
						selectedMods.add(mod);
					}
					modList.repaint();
				}
			}
		});

		mainPanel.add(new JScrollPane(modList), BorderLayout.CENTER);

		JPanel bottom = new JPanel(new BorderLayout());
		JPanel nav = new JPanel();
		leftBtn = new JButton("<");
		leftBtn.addActionListener(e -> changePage(currentPage - 1));
		nav.add(leftBtn);

		pageField = new JTextField("1", 3);
		pageField.setHorizontalAlignment(JTextField.CENTER);
		pageField.setEditable(false);
		nav.add(pageField);

		rightBtn = new JButton(">");
		rightBtn.addActionListener(e -> changePage(currentPage + 1));
		nav.add(rightBtn);

		nav.add(new JLabel("Per Page:"));
		perPageCombo = new JComboBox<>(new Integer[] {10, 20, 50, 100});
		perPageCombo.setSelectedItem(perPage);
		perPageCombo.addActionListener(e -> {
			perPage = (Integer) perPageCombo.getSelectedItem();
			currentPage = 1;
			fetchMods();
		});
		nav.add(perPageCombo);
		bottom.add(nav, BorderLayout.WEST);

		downloadBtn = new JButton("Download Selected");
		downloadBtn.addActionListener(e -> downloadSelected());
		JPanel dl = new JPanel();
		dl.add(downloadBtn);
		bottom.add(dl, BorderLayout.EAST);
		mainPanel.add(bottom, BorderLayout.SOUTH);

		JPanel statusBar = new JPanel(new BorderLayout());
		folderStatus = new JLabel("Folder: None");
		statusBar.add(folderStatus, BorderLayout.WEST);
		frame.add(mainPanel, BorderLayout.CENTER);
		frame.add(statusBar, BorderLayout.SOUTH);

		String lastFolder = prefs.get("lastFolder", null);
		if (lastFolder != null) {
			downloadFolder = new File(lastFolder);
			folderStatus.setText("Folder: " + lastFolder);
		}
		currentMC = prefs.get("lastMC", "1.21.4");
		currentLoader = prefs.get("lastLoader", "fabric");

		frame.setVisible(true);
		updateNavButtons();
		if (downloadFolder != null) {
			fetchMods();
		}
	}

	void showSettingsDialog() {
		JDialog dialog = new JDialog(frame, "Settings", true);
		dialog.setLayout(new GridLayout(0, 2, 5, 5));
		dialog.setSize(400, 200);
		dialog.setLocationRelativeTo(frame);

		JLabel folderLabel = new JLabel("Mod folder:");
		dialog.add(folderLabel);

		JButton chooseBtn = new JButton("Choose Folder");
		dialog.add(chooseBtn);

		chooseBtn.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setFileHidingEnabled(false);
			if (downloadFolder != null) chooser.setCurrentDirectory(downloadFolder);
			if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
				downloadFolder = chooser.getSelectedFile();
				prefs.put("lastFolder", downloadFolder.getAbsolutePath());
				folderStatus.setText("Folder: " + downloadFolder.getAbsolutePath());
			}
		});

		JLabel mcLabel = new JLabel("MC Version:");
		dialog.add(mcLabel);

		JComboBox<String> mcCombo = new JComboBox<>(new String[] {"1.21.4", "1.20.4", "1.19.4", "1.18.2", "1.17.1"});
		mcCombo.setSelectedItem(currentMC);
		dialog.add(mcCombo);

		JLabel loaderLabel = new JLabel("Loader:");
		dialog.add(loaderLabel);

		JComboBox<String> loaderCombo = new JComboBox<>(new String[] {"fabric", "forge", "quilt", "neoforge"});
		loaderCombo.setSelectedItem(currentLoader);
		dialog.add(loaderCombo);

		JButton saveBtn = new JButton("Save");
		saveBtn.addActionListener(e -> {
			currentMC = (String) mcCombo.getSelectedItem();
			currentLoader = (String) loaderCombo.getSelectedItem();
			prefs.put("lastMC", currentMC);
			prefs.put("lastLoader", currentLoader);
			dialog.dispose();
		});
		dialog.add(new JLabel());
		dialog.add(saveBtn);

		dialog.setVisible(true);
	}

	void fetchMods() {
		fetchBtn.setEnabled(false);
		listModel.clear();

		pageSelections.put(currentPage, new HashSet<>(selectedMods));
		new FetchWorker(currentPage, perPage, currentMC, currentLoader, listModel,
						selectedMods, modList, iconExecutor, this::updateNavButtons).execute();
	}

	void changePage(int newPage) {
		if (newPage < 1 || newPage > maxPages) return;

		pageSelections.put(currentPage, new HashSet<>(selectedMods));

		currentPage = newPage;

		if (pageSelections.containsKey(currentPage)) {
			Set<ModEntry> saved = pageSelections.get(currentPage);
			selectedMods.clear();
			selectedMods.addAll(saved);
		} else {
			selectedMods.clear();
		}

		pageField.setText(String.valueOf(currentPage));
		fetchMods();
	}

	void updateNavButtons() {
		leftBtn.setEnabled(currentPage > 1);
		rightBtn.setEnabled(currentPage < maxPages);
		pageField.setText(String.valueOf(currentPage));
	}

	void downloadSelected() {
		if (downloadFolder == null || !downloadFolder.isDirectory()) {
			JOptionPane.showMessageDialog(frame, "Choose a valid download folder first");
			return;
		}

		pageSelections.put(currentPage, new HashSet<>(selectedMods));

		Set<ModEntry> allSelected = new HashSet<>();
		for (Set<ModEntry> selections : pageSelections.values()) {
			allSelected.addAll(selections);
		}

		if (allSelected.isEmpty()) return;
		new ConfirmFrame(new ArrayList<>(allSelected), selectedMods, pageSelections,
						 downloadFolder, frame, currentMC, currentLoader).setVisible(true);
	}
}