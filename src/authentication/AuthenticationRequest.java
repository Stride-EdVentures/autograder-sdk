package authentication;

import com.google.api.client.util.Key;

public class AuthenticationRequest {
    @Key
    public String email;

    @Key
    public String password;

    public AuthenticationRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}