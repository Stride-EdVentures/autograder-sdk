package submissions;

import com.google.api.client.util.Key;

public class AssignmentSubmissionResponse {

	@Key
    public String id;
	
	@Key("profile_id")
    public String profileId;
	
	@Key("assignment_id")
    public String assignmentId;
    

    @Key("file_name")
    public String fileName;
    
    @Key
    public Integer version;
    
    @Key
    public String created_at;

}
