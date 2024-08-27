package classes;

import assignments.AutograderAssignment;

import java.util.Arrays;

import com.google.api.client.util.Key;

public class AutograderClass {
    @Key
    public String id;

    @Key
    public String name;
    
    @Key
    public String quarter;

    @Key("assignment")
    public AutograderAssignment[] assignments;
    
    @Override
    public String toString() {
       return String.format("name:%s, quarter:%s, assignments[]:%s", name, quarter, Arrays.toString(assignments));
    }
}
