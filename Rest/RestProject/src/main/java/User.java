import java.util.*;

class User {

    private Integer id;
    private String username;
    private String password;
    private HashMap<Integer, Image> images = new HashMap<>();
    private Integer counter=0;

    public User(int id, String username, String password) {
        super();
        this.id = id;
        this.username = username;
        this.password= password;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer addImage(Image image){
        synchronized (this) {
            image.setKey(counter);
            image.setPath(counter);
            counter++;
        }
        this.images.put(image.getKey(),image);
        return image.getKey();
    }

    public void deleteImage(Integer key){
        this.images.remove(key);
    }

    public Collection<Image> getAllImages(){
        List<Image> images= new LinkedList<>(this.images.values());
        return images;
    }

    public void setImages(HashMap<Integer, Image> map) {
        this.images = map;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof User)) return false;
        User user = (User) obj;
        if (user.getUsername().equals(this.username) && user.getPassword().equals(this.password)){
            return true;
        }
        return false;
    }

}
