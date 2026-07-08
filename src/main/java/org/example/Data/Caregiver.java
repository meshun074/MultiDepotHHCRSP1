package org.example.Data;

import java.util.Set;

public class Caregiver {
    private String id;
    private Set<String> abilities;
    private int distance_matrix_index = -1;
    private String starting_point_id;
    private double[] working_shift;

    private int cacheId = -1;
    private int cacheStartingPoint = -1;

    public String getId() {
        return id;
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

    public double[] getWorking_shift() {
        return working_shift;
    }

    public int getCacheId() {
        return cacheId;
    }

    public int getCacheStartingPoint() {
        return cacheStartingPoint;
    }

    void assignCacheId(int cacheId) {
        this.cacheId = cacheId;
    }

    void assignCacheStartingPoint(int cacheStartingPoint) {
        this.cacheStartingPoint = cacheStartingPoint;
    }

    void assignDistanceMatrixIndex(int distanceMatrixIndex) {
        this.distance_matrix_index = distanceMatrixIndex;
    }
}
