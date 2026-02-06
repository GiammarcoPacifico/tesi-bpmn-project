package tesiproject.bpmn.model;

public class Event extends Node{

    private EventType type;

    public Event() {}

    // Definisco un evento assegnandogli un id, un nome, la riga da cui è ottenuto nel xml e il tipo
    public Event(String id, String name, String laneId, EventType type) {
        super(id, name, laneId);
        this.type = type;
    }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
}
