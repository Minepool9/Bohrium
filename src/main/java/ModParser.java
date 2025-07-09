import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moandjiezana.toml.Toml;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ModParser {
    public static class ModInfo {
        public String id;
        public String name;
        public String description;
        public String minecraftVersion;
        public List<Dependency> dependencies = new ArrayList<>();
        public BufferedImage icon;
    }
    public static class Dependency {
        public String modId;
        public boolean mandatory;
        public String versionRange;
        public String side;
        public Dependency(String modId, boolean mandatory, String versionRange, String side) {
            this.modId = modId;
            this.mandatory = mandatory;
            this.versionRange = versionRange;
            this.side = side;
        }
    }
    public static ModInfo parse(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            if (jar.getEntry("fabric.mod.json") != null) return parseFabric(jar, "fabric.mod.json");
            if (jar.getEntry("quilt.mod.json") != null) return parseFabric(jar, "quilt.mod.json");
            if (jar.getEntry("META-INF/mods.toml") != null) return parseForgeToml(jar);
            if (jar.getEntry("mcmod.info") != null) return parseLegacy(jar);
        }
        return null;
    }
    private static ModInfo parseFabric(JarFile jar, String path) throws IOException {
        JsonNode root = new ObjectMapper().readTree(jar.getInputStream(jar.getEntry(path)));
        ModInfo mi = new ModInfo();
        mi.id = root.path("id").asText();
        mi.name = root.path("name").asText();
        mi.description = root.path("description").asText();
        mi.minecraftVersion = root.path("environment").asText().isEmpty()
            ? root.path("requires").path("minecraft").asText()
            : root.path("environment").asText();
        JsonNode deps = root.path("depends");
        deps.fieldNames().forEachRemaining(k ->
            mi.dependencies.add(new Dependency(k, true, deps.path(k).asText(), "BOTH"))
        );
        JsonNode suggests = root.path("suggests");
        suggests.fieldNames().forEachRemaining(k ->
            mi.dependencies.add(new Dependency(k, false, suggests.path(k).asText(), "BOTH"))
        );
        if (root.has("icon")) mi.icon = ImageIO.read(jar.getInputStream(jar.getEntry(root.path("icon").asText())));
        return mi;
    }
    private static ModInfo parseForgeToml(JarFile jar) throws IOException {
        InputStream in = jar.getInputStream(jar.getEntry("META-INF/mods.toml"));
        Toml toml = new Toml().read(new InputStreamReader(in));
        List<Toml> mods = toml.getTables("mods");
        Toml m = mods.get(0);
        ModInfo mi = new ModInfo();
        mi.id = m.getString("modId");
        mi.name = m.getString("displayName");
        mi.description = m.getString("description");
        mi.minecraftVersion = toml.getString("loaderVersion");
        String logo = m.getString("logoFile");
        if (logo != null) mi.icon = ImageIO.read(jar.getInputStream(jar.getEntry(logo)));
        List<Toml> deps = toml.getTables("dependencies." + mi.id);
        for (Toml d : deps) {
            mi.dependencies.add(new Dependency(
                d.getString("modId"),
                d.getBoolean("mandatory", true),
                d.getString("versionRange"),
                d.getString("side", "BOTH")
            ));
        }
        return mi;
    }
    private static ModInfo parseLegacy(JarFile jar) throws IOException {
        JsonNode arr = new ObjectMapper().readTree(jar.getInputStream(jar.getEntry("mcmod.info")));
        JsonNode m = arr.get(0);
        ModInfo mi = new ModInfo();
        mi.id = m.path("modid").asText();
        mi.name = m.path("name").asText();
        mi.description = m.path("description").asText();
        mi.minecraftVersion = m.path("mcversion").asText();
        String logo = m.path("logoFile").asText();
        if (!logo.isEmpty()) mi.icon = ImageIO.read(jar.getInputStream(jar.getEntry(logo)));
        JsonNode deps = m.path("dependencies");
        if (deps.isObject()) {
            deps.fieldNames().forEachRemaining(k ->
                mi.dependencies.add(new Dependency(k, true, deps.path(k).asText(), "BOTH"))
            );
        }
        return mi;
    }
}