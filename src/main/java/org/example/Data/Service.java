package org.example.Data;

public class Service {
    private String id;
    private double default_duration;
    private String type;
    private int cacheId = -1;

    public String getId() {
        return id;
    }

    public double getDefault_duration() {
        return default_duration;
    }

    public String getType() {
        return type;
    }

    public int getCacheId() {
        return cacheId;
    }

    void assignCacheId(int cacheId) {
        this.cacheId = cacheId;
    }
}
