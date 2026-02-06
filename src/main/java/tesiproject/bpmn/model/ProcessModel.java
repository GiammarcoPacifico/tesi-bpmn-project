package tesiproject.bpmn.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProcessModel {

    private String id;
    private String name;
    private String namespace;

    private final List<Participant> participants = new ArrayList<>();
    private final List<Lane> lanes = new ArrayList<>();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Flow> flows = new ArrayList<>();

    private ModelIndex index;

    // --- getters/setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public List<Participant> getParticipants() { return participants; }
    public List<Lane> getLanes() { return lanes; }
    public List<Node> getNodes() { return nodes; }
    public List<Flow> getFlows() { return flows; }

     //BuildIndex() va chiamato dopo parsing
    public ModelIndex getIndex() {
        if (index == null) {
            // fallback safe: costruisci al volo (non ideale, ma evita crash)
            index = new ModelIndex(nodes, flows);
        }
        return index;
    }

    // Build index after parsing
    public void buildIndex() {
        this.index = new ModelIndex(nodes, flows);
    }

    // --- convenience ---
    public List<Activity> getActivities() {
        return nodes.stream().filter(n -> n instanceof Activity).map(n -> (Activity) n).toList();
    }

    public List<Gateway> getGateways() {
        return nodes.stream().filter(n -> n instanceof Gateway).map(n -> (Gateway) n).toList();
    }

    public List<Event> getEvents() {
        return nodes.stream().filter(n -> n instanceof Event).map(n -> (Event) n).toList();
    }

    public long countActivities() { return getActivities().size(); }
    public long countGateways() { return getGateways().size(); }

    public long countMessageFlows() {
        return flows.stream().filter(f -> f instanceof MessageFlow).count();
    }

    public boolean containsKeyword(String keywordLowercase) {
        if (keywordLowercase == null || keywordLowercase.isBlank()) return false;

        String k = keywordLowercase.toLowerCase(Locale.ROOT);

        // 1) Nodes
        boolean inNodes = nodes.stream().map(Node::getName).filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains(k));

        if (inNodes) return true;

        // 2) Lanes (nome lane)
        boolean inLanes = lanes.stream().map(Lane::getName).filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains(k));

        if (inLanes) return true;

        // 3) Participants (nome participant)
        boolean inParticipants = participants.stream().map(Participant::getName).filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains(k));

        if (inParticipants) return true;

        // 4) Flows: name + condition (sequenceFlow), name (messageFlow)
        boolean inFlows = flows.stream().anyMatch(f -> {
            if (f == null) return false;

            // MessageFlow e SequenceFlow hanno name comune
            String name = f.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains(k)) return true;

            // Condition solo per SequenceFlow
            if (f instanceof SequenceFlow sf) {
                String cond = sf.getCondition();
                return cond != null && cond.toLowerCase(Locale.ROOT).contains(k);
            }

            return false;
        });

        return inFlows;
    }

    public List<Gateway> getGatewaysOfType(GatewayType type) {
        if (type == null) return List.of();
        return getGateways().stream().filter(g -> g.getType() == type).toList();
    }
}