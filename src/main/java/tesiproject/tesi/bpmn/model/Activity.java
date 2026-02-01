package tesiproject.tesi.bpmn.model;

public class Activity extends Node{

    private ActivityType type;

    public Activity() {}

    // Definisco un'attività assegnandole un id, un nome, la riga da cui è ottenuto nel xml e il tipo
    public Activity(String id, String name, String laneId, ActivityType type) {
        super(id, name, laneId);
        this.type = type;
    }

    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
}
