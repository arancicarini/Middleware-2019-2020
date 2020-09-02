import com.google.gson.Gson;

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

    public Integer addImage(Image image) throws ImageException {
        for (Image image1: images.values()){
            if(image1.getTitle().equals(image.getTitle())){
                throw new ImageException("image already exist for this user");
            }
        }
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

    @Override
    public String toString() {
        return new Gson().toJson( this);
    }
}
