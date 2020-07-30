import sun.text.resources.CollationData;

import java.util.Collection;

public interface ImageService {

    public void addImage(Image image, Integer userID);

    public void deleteImage(Image image, Integer userID);

    public Collection<Image> getUserImages(Integer userID);

    public Image getImage(String key);


}
