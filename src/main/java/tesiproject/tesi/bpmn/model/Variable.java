package tesiproject.tesi.bpmn.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Variable {

    private String name;
    private VarType type;
    private String unit;          // "EUR", "kWh", "H", "gCO2", ...
    private Object value;         // INT->Integer, DOUBLE->Double, MONEY->BigDecimal, BOOLEAN->Boolean, ...
    private VarCategory category;
    private VarSource source;
    private Double confidence;

    public Variable() {}

    public Variable(String name,
                    VarType type,
                    String unit,
                    Object value,
                    VarCategory category,
                    VarSource source,
                    Double confidence) {
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.value = value;
        this.category = category;
        this.source = source;
        this.confidence = confidence;
    }

    public static Variable intVar(String name, Integer value, VarCategory cat, VarSource src) {
        return new Variable(name, VarType.INT, null, value, cat, src, null);
    }

    public static Variable doubleVar(String name, Double value, String unit, VarCategory cat, VarSource src) {
        return new Variable(name, VarType.DOUBLE, unit, value, cat, src, null);
    }

    public static Variable boolVar(String name, Boolean value, VarCategory cat, VarSource src) {
        return new Variable(name, VarType.BOOLEAN, null, value, cat, src, null);
    }

    public static Variable moneyVar(String name, BigDecimal value, VarCategory cat, VarSource src) {
        return new Variable(name, VarType.MONEY, "EUR", value, cat, src, null);
    }

    public static Variable moneyVar(String name, BigDecimal value, String currencyUnit, VarCategory cat, VarSource src) {
        return new Variable(name, VarType.MONEY, currencyUnit == null ? "EUR" : currencyUnit, value, cat, src, null);
    }

    public Integer asInt() {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    public Double asDouble() {
        if (value == null) return null;
        if (value instanceof Double d) return d;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    public BigDecimal asBigDecimal() {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(value));
    }

    public Boolean asBoolean() {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
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

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String toHumanString() {
        String valueStr = (value == null) ? "?" : String.valueOf(value);
        String unitStr = (unit == null || unit.isBlank()) ? "" : " " + unit;
        return name + " = " + valueStr + unitStr;
    }

    @Override
    public String toString() {
        return "Variable{" + "name='" + name + '\'' + ", type=" + type + ", value=" + value + (unit != null ? (", unit='" + unit + '\'') : "") +
                ", category=" + category + ", source=" + source + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable other)) return false;
        return Objects.equals(name, other.name) && type == other.type && Objects.equals(unit, other.unit) && Objects.equals(value, other.value)
                && category == other.category && source == other.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, unit, value, category, source);
    }
}
