package storage;

import com.google.api.client.util.Key;

public class SubmissionMetadata {
    @Key
    public int size;

    @Key
    public String mimetype;

    @Key
    public String cacheControl;
}
