
import java.util.Collection;

public interface ImageService {

    public String addImage(Image image, int userID);

    public void deleteImage(String key, Integer userID);

    public Collection<Image> getUserImages(Integer userID);

    public Image getImage(String key);


}
