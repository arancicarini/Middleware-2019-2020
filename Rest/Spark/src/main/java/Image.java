import com.google.gson.Gson;

import java.nio.file.Path;

public class Image {
    private Integer key;
    private String title;
    private String path;


    public Image(String imageName, String path) {
        this.title=imageName;
    }

    public Image(String name) {
        this.title=name;
    }

    public Integer getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(Integer key) {
        this.path = "http://localhost:4567/images/" + String.valueOf(key);
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "{ key='" + key + '\'' +
                ", title='" + title + '\''+
                ", http://127.0.0.1:4567/images/" + key +
                '}';
    }
}

