
public class Image {
    private Integer key;
    private String title;
    private String path;

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
        this.path = "http://localhost:4567/images/" + key;
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

