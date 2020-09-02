
import java.util.Collection;

public interface ImageService {

    public Integer addImage(Image image, int userID) throws ImageException;

    public void deleteImage(Integer key, Integer userID);

    public Collection<Image> getUserImages(Integer userID);

    public Image getImage(String key);


}
