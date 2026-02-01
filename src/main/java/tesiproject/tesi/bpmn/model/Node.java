package tesiproject.tesi.bpmn.model;

public abstract class Node {

    private String id;
    private String name;
    private String laneId; // Opzionale

    protected Node() {}

    protected Node(String id, String name, String laneId) {
        this.id = id;
        this.name = name;
        this.laneId = laneId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLaneId() { return laneId; }
    public void setLaneId(String laneId) { this.laneId = laneId; }
}
