package tesiproject.bpmn.model;

public class Gateway extends Node{

    private GatewayType type;

    public Gateway() {}

    public Gateway(String id, String name, String laneId, GatewayType type) {
        super(id, name, laneId);
        this.type = type;
    }

    public GatewayType getType() { return type; }
    public void setType(GatewayType type) { this.type = type; }
}
