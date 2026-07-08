package org.example.Data;

public class Offices {
    private String id;
    private double[] location;
    private int cacheId = -1;

    public String getId() {
        return id;
    }

    public double[] getLocation() {
        return location;
    }

    public int getCacheId() {
        return cacheId;
    }

    void assignCacheId(int cacheId) {
        this.cacheId = cacheId;
    }
}
