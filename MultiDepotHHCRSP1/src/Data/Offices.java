package Data;

public class Offices {
    private String id;
    private String[] location;
    private int cacheId = -1;

    public String getId() {
        return id;
    }

    public String[] getLocation() {
        return location;
    }

    public void setId(String id) {
        this.id = id;
        this.cacheId = Integer.parseInt(id.substring(1));
    }

    public int getCacheId() {return cacheId;}
}
