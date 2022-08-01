package authentication;

import com.google.api.client.util.Key;

public class User {
    @Key
    public String id;

    @Key
    public String aud;

    @Key
    public String role;

    @Key
    public String email;

    @Key
    public String email_confirmed_at;

    @Key
    public String confirmed_at;

    @Key
    public String last_sign_in_at;

    @Key
    public String created_at;

    @Key
    public String updated_at;
}
