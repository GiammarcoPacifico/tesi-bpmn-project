package tesiproject.tesi.controller;

import tesiproject.tesi.bpmn.model.VarCategory;
import tesiproject.tesi.bpmn.model.VarSource;
import tesiproject.tesi.bpmn.model.VarType;
import tesiproject.tesi.bpmn.model.Variable;

public class VariableDto {

    private String name;
    private VarType type;
    private String unit;
    private Object value;
    private VarCategory category;
    private VarSource source;

    public VariableDto() {}

    public static VariableDto from(Variable v) {
        VariableDto dto = new VariableDto();
        dto.setName(v.getName());
        dto.setType(v.getType());
        dto.setUnit(v.getUnit());
        dto.setValue(v.getValue());
        dto.setCategory(v.getCategory());
        dto.setSource(v.getSource());
        return dto;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public VarType getType() { return type; }
    public void setType(VarType type) { this.type = type; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public VarCategory getCategory() { return category; }
    public void setCategory(VarCategory category) { this.category = category; }

    public VarSource getSource() { return source; }
    public void setSource(VarSource source) { this.source = source; }
}
