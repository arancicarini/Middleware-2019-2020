import java.util.Collection;
import java.util.HashMap;

public class ServiceImplementation implements UserService, ImageService {
    private final HashMap<Integer, User> userMap;

    public ServiceImplementation() {
        userMap = new HashMap<>();
    }

    @Override
    public void addUser(User user) {
        userMap.put(user.getId(), user);
    }

    @Override
    public void registerUser(User user) {

    }

    @Override
    public void login(User user) {

    }

    @Override
    public Collection<User> getUsers() {
        return userMap.values();
    }

    @Override
    public User getUser(int id) {
        return userMap.get(id);
    }

    @Override
    public User editUser(User forEdit) throws UserException {
        try {
            if (forEdit.getId() == null)
                throw new UserException("ID cannot be blank");

            User toEdit = userMap.get(forEdit.getId());

            if (toEdit == null)
                throw new UserException("User not found");

            /**if (forEdit.getUsername() != null) {
                toEdit.setUsername(forEdit.getUsername());
            }**/

            if (forEdit.getId() != null) {
                toEdit.setId(forEdit.getId());
            }

            return toEdit;
        } catch (Exception ex) {
            throw new UserException(ex.getMessage());
        }
    }

    @Override
    public void deleteUser(int id) {
        userMap.remove(id);
    }

    @Override
    public boolean userExist(int id) {
        return userMap.containsKey(id);
    }

    @Override
    public String addImage(Image image, int userID) {
        image.setKey("AAA");
        userMap.get(userID).addImage(image);
        return image.getKey();
    }

    @Override
    public void deleteImage(String key, Integer userID) {
        userMap.get(userID).deleteImage(key);
    }

    @Override
    public Collection<Image> getUserImages(Integer userID) {
        System.out.println(userMap.get(userID).getAllImages());
        return userMap.get(userID).getAllImages();
    }

    @Override
    public Image getImage(String key) {
        return null;
    }

}