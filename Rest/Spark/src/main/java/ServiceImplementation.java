import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ServiceImplementation implements UserService, ImageService {
    private final HashMap<Integer, User> userMap;
    private int counter=1;

    public ServiceImplementation() {
        userMap = new HashMap<>();
    }

    @Override
    public Integer registerUser(User user) throws UserException {
        if ( user.getUsername() == null || user.getPassword() == null){
            throw new UserException();
        }
        synchronized(ServiceImplementation.class){
            user.setId(counter);
            counter++;
        }
        String hash = produceSHA1(user.getPassword());
        user.setPassword(hash);
        userMap.put(user.getId(), user);
        return user.getId();
    }

    @Override
    public String login(User user) throws UserException {
        if ( user.getPassword() == null || user.getUsername() == null ){
            throw new UserException();
        }

        String hash = produceSHA1(user.getPassword());
        user.setPassword(hash);
        List<User> list = new LinkedList<>(userMap.values());
        boolean userExists = false;
        for ( User registeredUser : list){
            if (registeredUser.equals(user)) userExists = true ;
        }
        if (!userExists) throw new UserException();
        return createToken(user.getUsername());
    }

    @Override
    public void authenticate (String token, String username) throws UserException{
        String secret = readSecret();

        String required = secret.concat(username);
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(required))
                    .withIssuer("auth0")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);
        }catch (TokenExpiredException e){
            throw new UserException();
        } catch (JWTVerificationException exception){
            throw new UserException();
            //Invalid signature/claims
        }
    }

    @Override
    public Collection<User> getUsers() {
        List<User> users = new LinkedList<>(userMap.values());
        return users;
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

            if (forEdit.getUsername() != null) {
                toEdit.setUsername(forEdit.getUsername());
            }

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


    ////// support functions

    private static String produceSHA1(String password){
        String key = password;
        MessageDigest digest;
        byte[] hash;
        StringBuffer hexHash = new StringBuffer();
        try {
            // Create the SHA-1 of the nodeidentifier
            digest = MessageDigest.getInstance("SHA-1");
            hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            // Convert hash bytes into StringBuffer ( via integer representation)
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexHash.append('0');
                hexHash.append(hex);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexHash.toString();
    }

    private static String readSecret(){
        FileReader fr = null;
        Scanner fileScanner = null;
        String key = "default_key";
        try {
            fr = new FileReader("HMACkey.txt");
            fileScanner = new Scanner(fr);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                key = line;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fr.close();
            } catch (NullPointerException e) {
            } catch (IOException e) {
            }
            try {
                fileScanner.close();
            } catch (NullPointerException e) {
            }
        }
        return key;
    }

    private static String createToken(String username){
        String secret = readSecret();
        String token = "default_token" + username;
        String required = secret.concat(username);
        try {
            Algorithm algorithm = Algorithm.HMAC256(required);
            token = JWT.create()
                    .withIssuer("auth0")
                    .withExpiresAt(new Date(System.currentTimeMillis() + (600 * 1000)))
                    .sign(algorithm);
        } catch (JWTCreationException exception){
            //Invalid Signing configuration / Couldn't convert Claims.
        }

        return token;
    }

}