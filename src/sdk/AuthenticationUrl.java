package sdk;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

public class AuthenticationUrl extends GenericUrl {
    @Key
    public String grant_type;

    public AuthenticationUrl(String encodedUrl) {
        super(encodedUrl);
    }
}
