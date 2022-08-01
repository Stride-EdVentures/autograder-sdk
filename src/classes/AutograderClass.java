package classes;

import assignments.AutograderAssignment;
import com.google.api.client.util.Key;

public class AutograderClass {
    @Key
    public String id;

    @Key
    public String name;

    @Key
    public AutograderAssignment[] assignments;
}
