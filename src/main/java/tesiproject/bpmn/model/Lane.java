package tesiproject.bpmn.model;

import java.util.ArrayList;
import java.util.List;

public class Lane {

    private String id;
    private String name;
    private String participantId; // Opzionale
    private final List<String> nodeRefs = new ArrayList<>();

    public Lane() {}

    public Lane(String id, String name, String participantId) {
        this.id = normalize(id);
        this.name = normalize(name);
        this.participantId = normalize(participantId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = normalize(id); }

    public String getName() { return name; }
    public void setName(String name) { this.name = normalize(name); }

    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = normalize(participantId); }

    /*
     Lista live e mutabile (serve al parser)
     */
    public List<String> getNodeRefs() { return nodeRefs; }

    /* Aggiunge un riferimento nodo in modo safe. */
    public void addNodeRef(String nodeId) {
        String n = normalize(nodeId);
        if (n == null) return;
        nodeRefs.add(n);
    }

    /* Utility: trim + null se vuota. */
    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
