package assignments;

import com.google.api.client.util.Key;

public class AutograderAssignment {
    @Key
    public String id;

    @Key
    public String name;

    @Key
    public String description;

    @Key
    public String[] required_files;

    @Key
    public String due_date;

    @Key
    public String class_id;
}
