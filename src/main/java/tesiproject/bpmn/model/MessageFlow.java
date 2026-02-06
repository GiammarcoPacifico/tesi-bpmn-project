package tesiproject.bpmn.model;

public class MessageFlow extends Flow{

    private String sourceRef;
    private String targetRef;

    public MessageFlow() {}

    public MessageFlow(String id, String name, String sourceRef, String targetRef) {
        super(id, name);
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
    }

    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public String getTargetRef() { return targetRef; }
    public void setTargetRef(String targetRef) { this.targetRef = targetRef; }
}
