package tesiproject.bpmn.model;

public class SequenceFlow extends Flow{

    private String sourceId;
    private String targetId;
    private String condition;

    public SequenceFlow() {}

    public SequenceFlow(String id, String name, String sourceId, String targetId, String condition) {
        super(id, name);
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.condition = condition;
    }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
