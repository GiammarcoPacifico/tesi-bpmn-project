package tesiproject.tesi.bpmn.model;

public abstract class Flow {

    private String id;
    private String name;

    protected Flow() {}

    protected Flow(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
