// DownloadWorker.java
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadWorker extends SwingWorker<Void, String> {
    private final List<ModEntry> mods;
    private final DownloadFrame df;
    private final File downloadFolder;
    private final JFrame mainFrame;
    private final String currentMC;
    private final String currentLoader;
    private long totalBytes;

    public DownloadWorker(List<ModEntry> selected, DownloadFrame frame, File downloadFolder, 
                         JFrame mainFrame, String currentMC, String currentLoader) {
        mods = new ArrayList<>(selected);
        df = frame;
        this.downloadFolder = downloadFolder;
        this.mainFrame = mainFrame;
        this.currentMC = currentMC;
        this.currentLoader = currentLoader;
    }

    private String fetchContent(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Bohrium/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    protected Void doInBackground() {
        Map<ModEntry, String> fileUrls = new HashMap<>();
        totalBytes = 0;
        try {
            for (ModEntry e : mods) {
                JSONArray versions = new JSONArray(fetchContent("https://api.modrinth.com/v2/project/" + e.id + "/version"));
                JSONObject sel = versions.getJSONObject(0);
                for (int i = 0; i < versions.length(); i++) {
                    JSONObject v = versions.getJSONObject(i);
                    if (v.getJSONArray("game_versions").toList().contains(currentMC) && 
                        v.getJSONArray("loaders").toList().contains(currentLoader)) {
                        sel = v;
                        break;
                    }
                }
                JSONObject file = sel.getJSONArray("files").getJSONObject(0);
                String url = file.getString("url");
                fileUrls.put(e, url);
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("User-Agent", "Bohrium/1.0");
                conn.connect();
                totalBytes += conn.getContentLengthLong();
                conn.disconnect();
            }
            long downloaded = 0;
            for (ModEntry e : mods) {
                publish("Downloading " + e.title + "...\n");
                String url = fileUrls.get(e);
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Bohrium/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (InputStream in = conn.getInputStream(); 
                     FileOutputStream fos = new FileOutputStream(downloadFolder.toPath().resolve(Paths.get(new URI(url).getPath()).getFileName()).toFile())) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                        downloaded += r;
                        df.updateProgressBytes(downloaded, totalBytes);
                    }
                }
                publish("Completed: " + e.title + "\n");
            }
        } catch (Exception ex) {
            publish("Error: " + ex.getMessage() + "\n");
        }
        return null;
    }

    protected void process(List<String> logs) {
        for (String log : logs) {
            df.log(log);
        }
    }

    protected void done() {
        df.dispose();
        JOptionPane.showMessageDialog(mainFrame, "Downloaded mods to:\n" + downloadFolder.getAbsolutePath(), "Downloads Complete", JOptionPane.INFORMATION_MESSAGE);
    }
}