package profiles;

import classes.AutograderClass;
import com.google.api.client.util.Key;

public class ProfileResponse {
    @Key
    public String id;

    @Key
    public boolean is_teacher;

    @Key
    public AutograderClass[] classes;
}
