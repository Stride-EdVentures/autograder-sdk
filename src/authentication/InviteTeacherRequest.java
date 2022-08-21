package authentication;

import com.google.api.client.util.Key;

public class InviteTeacherRequest {
    @Key
    public String currentEmail;

    @Key
    public String email;

    public InviteTeacherRequest(String currentEmail, String email) {
        this.currentEmail = currentEmail;
        this.email = email;
    }
}