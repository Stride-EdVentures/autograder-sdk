package enrollments;

import com.google.api.client.util.Key;

import classes.AutograderClass;
import profiles.ProfileResponse;

public class EnrollmentResponse {
    @Key
    public String type;
    
    @Key("class")
    public AutograderClass singleClass;
    
    @Key("profile")
    public ProfileResponse profile;

}
