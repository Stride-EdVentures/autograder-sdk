package storage;

import com.google.api.client.util.Key;

public class FileRequest {
    @Key
    public int limit;

    @Key
    public int offset;

    @Key
    public SortOptions sortBy;

    @Key
    public String prefix;
}
