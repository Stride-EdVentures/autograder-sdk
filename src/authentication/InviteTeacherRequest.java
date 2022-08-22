package authentication;

import com.google.api.client.util.Key;

public class InviteTeacherRequest {
    @Key
    public String email;

    @Key
    public String newTeacherEmail;

    public InviteTeacherRequest(String email, String newTeacherEmail) {
        this.email = email;
        this.newTeacherEmail = newTeacherEmail;
    }
}