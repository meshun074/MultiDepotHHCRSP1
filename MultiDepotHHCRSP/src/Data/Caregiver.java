package Data;

import java.util.Set;

public class Caregiver {
    private String id;
    private Set<String> abilities;
    private int distance_matrix_index;
    private String starting_point_id;
    private double[] working_shift;
    private int cacheId;
    private int cacheStartingPoint;

    public String getId() {
        return id;
    }
    // This method will be called by Jackson after deserialization
    public void setId(String id) {
        this.id = id;
        // Recalculate cacheId now that id is set
        this.cacheId = Integer.parseInt(id.substring(1));
    }

    public void setStarting_point_id(String starting_point_id) {
        this.starting_point_id = starting_point_id;
        this.cacheStartingPoint = Integer.parseInt(starting_point_id.substring(1));
    }
    public int getCacheId() {
        return cacheId;
    }
    public Set<String> getAbilities() {
        return abilities;
    }
    public int getDistance_matrix_index() {
        return distance_matrix_index;
    }
    public String getStarting_point_id() {
        return starting_point_id;
    }
    public int getCacheStartingPoint() {
        return cacheStartingPoint;
    }
    public double[] getWorking_shift() {
        return working_shift;
    }

}
