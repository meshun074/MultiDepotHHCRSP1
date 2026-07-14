package Data;

public class Service {
    private String id;
    private String default_duration;
    private String type;
    private int cacheId = -1;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        this.cacheId = Integer.parseInt(id.substring(1));
    }

    public int getCacheId() {
        return cacheId;
    }
    public String getDefault_duration() {
        return default_duration;
    }
    public String getType() {return type;}

}

