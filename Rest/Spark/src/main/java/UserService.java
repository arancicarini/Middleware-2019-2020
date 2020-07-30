import java.util.Collection;

public interface UserService {

    public void addUser (User user);

    public void registerUser(User user);

    public void login(User user);

    public Collection<User> getUsers ();
    public User getUser (int id);

    public User editUser (User user)
            throws UserException;

    public void deleteUser (int id);

    public boolean userExist (int id);
}