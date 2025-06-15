// ModEntry.java
import javax.swing.ImageIcon;
import java.util.Objects;

public class ModEntry {
    String title, slug;
    ImageIcon icon;
    String id;
    
    public ModEntry(String t, String s, String id) {
        title = t;
        slug = s;
        this.id = id;
    }
    
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModEntry modEntry = (ModEntry) o;
        return Objects.equals(id, modEntry.id);
    }
    
    public int hashCode() {
        return Objects.hash(id);
    }
    
    public String toString() {
        return title;
    }
}