public class Image {
    private String key;
    private String title;
    private String path;

    public Image(String key, String title, String path){
        this.key=key;
        this.title=title;
        this.path= path;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

