import java.util.Collection;
import java.util.HashMap;

class User {

    private Integer id;
    private String name;
    //private String username;
    //private String password;
    private HashMap<String, Image> images;

    /**public User(int id, String username, String password) {
        super();
        this.id = id;
        //this.username = username;
        //this.password= password;
        images = null;
    }**/

    public User(int id, String name) {
        this.id = id;
        this.name=name;
        this.images= new HashMap<>();
    }
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**public String getUsername() {
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
     * @return**/

    public void addImage(Image image){
        this.images.put(image.getKey(),image);
    }

    public void deleteImage(String key){
        this.images.remove(key);
    }

    public Collection<Image> getAllImages(){
        System.out.println(images.values());
        return this.images.values();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setImages(){
         this.images= new HashMap<>();
    }
}
