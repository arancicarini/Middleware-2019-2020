import com.google.gson.Gson;

import java.nio.file.Path;

public class Image {
    private String key;
    private String title;
    private Path path;


    public Image(String imageName, Path path) {
        this.title=imageName;
        this.path= path;
    }

    public Image(String name) {
        this.title=name;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public Path getPath() {
        return path;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "Image{" +
                "key='" + key + '\'' +
                ", title='" + title + '\'' +
                ", http://127.0.0.1:4567/images/" + key +
                '}';
    }
}

