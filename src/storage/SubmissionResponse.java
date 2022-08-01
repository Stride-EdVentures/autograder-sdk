package storage;

import com.google.api.client.util.Key;

public class SubmissionResponse {
    @Key
    public String name;

    @Key
    public String id;

    @Key
    public String updated_at;

    @Key
    public String created_at;

    @Key
    public String last_accessed_at;

    @Key
    public SubmissionMetadata metadata;
}
