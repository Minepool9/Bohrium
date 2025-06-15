// FetchWorker.java
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class FetchWorker extends SwingWorker<Void, ModEntry> {
    private final int currentPage;
    private final int perPage;
    private final String currentMC;
    private final String currentLoader;
    private final DefaultListModel<ModEntry> listModel;
    private final Set<ModEntry> selectedMods;
    private final JList<ModEntry> modList;
    private final ExecutorService iconExecutor;
    private final Runnable updateNavCallback;
    private int maxPages = Integer.MAX_VALUE;

    public FetchWorker(int currentPage, int perPage, String currentMC, String currentLoader,
                      DefaultListModel<ModEntry> listModel, Set<ModEntry> selectedMods,
                      JList<ModEntry> modList, ExecutorService iconExecutor, 
                      Runnable updateNavCallback) {
        this.currentPage = currentPage;
        this.perPage = perPage;
        this.currentMC = currentMC;
        this.currentLoader = currentLoader;
        this.listModel = listModel;
        this.selectedMods = selectedMods;
        this.modList = modList;
        this.iconExecutor = iconExecutor;
        this.updateNavCallback = updateNavCallback;
    }

    private String fetchContent(String urlStr) throws IOException {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Bohrium/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    protected Void doInBackground() {
        try {
            String raw = "[[\"project_type:mod\"],[\"versions:" + currentMC + "\"],[\"categories:" + currentLoader + "\"]]";
            String facets = URLEncoder.encode(raw, "UTF-8");
            int offset = (currentPage - 1) * perPage;
            JSONObject obj = new JSONObject(fetchContent("https://api.modrinth.com/v2/search?limit=" + perPage + "&offset=" + offset + "&facets=" + facets));
            maxPages = (int) Math.ceil((double) obj.getInt("total_hits") / perPage);
            JSONArray hits = obj.getJSONArray("hits");
            for (int i = 0; i < hits.length(); i++) {
                JSONObject mod = hits.getJSONObject(i);
                String projectId = mod.getString("project_id");
                ModEntry entry = new ModEntry(mod.getString("title"), mod.getString("slug"), projectId);
                publish(entry);
                if (mod.has("icon_url") && !mod.isNull("icon_url")) iconExecutor.submit(() -> loadIcon(entry, mod.getString("icon_url")));
            }
        } catch (Exception ignored) {}
        return null;
    }

    protected void process(List<ModEntry> chunks) {
        for (ModEntry entry : chunks) {
            listModel.addElement(entry);
            if (selectedMods.contains(entry)) {
                modList.repaint();
            }
        }
    }

    protected void done() {
        updateNavCallback.run();
        modList.repaint();
    }

    private void loadIcon(ModEntry entry, String url) {
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "Bohrium/1.0");
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                Image img = Toolkit.getDefaultToolkit().createImage(out.toByteArray()).getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                entry.icon = new ImageIcon(img);
                SwingUtilities.invokeLater(() -> { 
                    int idx = listModel.indexOf(entry); 
                    if (idx != -1) { 
                        listModel.setElementAt(entry, idx);
                        if (selectedMods.contains(entry)) {
                            modList.repaint();
                        }
                    } 
                });
            }
        } catch (Exception ignored) {}
    }
}