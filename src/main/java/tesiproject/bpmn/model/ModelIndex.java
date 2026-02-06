package tesiproject.bpmn.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelIndex {

    private final Map<String, Node> nodesById = new HashMap<>();
    private final Map<String, Flow> flowsById = new HashMap<>();

    // Solo per sequence flows (grafo interno)
    private final Map<String, List<SequenceFlow>> outgoing = new HashMap<>();
    private final Map<String, List<SequenceFlow>> incoming = new HashMap<>();

    public ModelIndex(List<Node> nodes, List<Flow> flows) {
        for (Node n : nodes) nodesById.put(n.getId(), n);
        for (Flow f : flows) flowsById.put(f.getId(), f);

        for (Flow f : flows) {
            if (f instanceof SequenceFlow sf) {
                outgoing.computeIfAbsent(sf.getSourceId(), k -> new ArrayList<>()).add(sf);
                incoming.computeIfAbsent(sf.getTargetId(), k -> new ArrayList<>()).add(sf);
            }
        }
    }

    public Node getNode(String id) { return nodesById.get(id); }

    public List<SequenceFlow> getOutgoing(String nodeId) {
        return outgoing.getOrDefault(nodeId, List.of());
    }

    public List<SequenceFlow> getIncoming(String nodeId) {
        return incoming.getOrDefault(nodeId, List.of());
    }
}
