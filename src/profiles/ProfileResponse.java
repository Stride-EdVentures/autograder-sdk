package profiles;

import classes.AutograderClass;
import com.google.api.client.util.Key;

public class ProfileResponse {
    @Key
    public String id;

    @Key
    public String email;

    @Key("auth_id")
    public String authId;

    
    //    @Key
//    public boolean is_teacher;

    // For Rest many-many relations
    @Key("class")
    public AutograderClass singleClass;
    
    public AutograderClass[] classes;
}
