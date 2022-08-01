package authentication;

import com.google.api.client.util.Key;

public class AuthenticationResponse {
    @Key
    public String access_token;

    @Key
    public String token_type;

    @Key
    public int expires_in;

    @Key
    public String refresh_token;

    @Key
    public User user;
}
