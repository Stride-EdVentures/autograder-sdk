package assignments;

import java.util.Arrays;

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
    
    @Override
    public String toString() {
       return String.format("name:%s, requiredFiles[]:%s", name, required_files == null ? "<none>" : Arrays.toString(required_files));
    }
}
