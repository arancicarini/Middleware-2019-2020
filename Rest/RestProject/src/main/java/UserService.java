


import java.util.Collection;

public interface UserService {

    public Integer registerUser(User user) throws UserException;

    public String login(User user) throws UserException;

    public void authenticate (String token, String username) throws UserException;

    public Collection<String> getUsers ();
    public User getUser (int id);

    public User editUser (User user)
            throws UserException;

    public void deleteUser (int id);

    public boolean userExist (int id);
}