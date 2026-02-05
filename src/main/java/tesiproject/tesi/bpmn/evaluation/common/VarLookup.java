package tesiproject.tesi.bpmn.evaluation.common;

import tesiproject.tesi.bpmn.model.Variable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VarLookup {

    private final Map<String, Variable> byName = new HashMap<>();

    public VarLookup(List<Variable> vars) {
        for (Variable v : vars) {
            if (v != null && v.getName() != null) byName.put(v.getName(), v);
        }
    }

    public Optional<BigDecimal> getNumber(String name) {
        Variable v = byName.get(name);
        if (v == null || v.getValue() == null || v.getType() == null) return Optional.empty();

        return switch (v.getType()) {
            case INT -> Optional.of(BigDecimal.valueOf(((Number) v.getValue()).longValue()));
            case DOUBLE -> Optional.of(BigDecimal.valueOf(((Number) v.getValue()).doubleValue()));
            case MONEY -> {
                Object val = v.getValue();
                if (val instanceof BigDecimal bd) yield Optional.of(bd);
                if (val instanceof Number n) yield Optional.of(BigDecimal.valueOf(n.doubleValue()));
                // Fallback "8.0 EUR"
                String s = val.toString().trim();
                String[] parts = s.split("\\s+");
                if (parts.length == 2 && parts[0].matches("[-+]?\\d+(\\.\\d+)?")) yield Optional.of(new BigDecimal(parts[0]));
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    public Optional<Integer> getInt(String name) {
        return getNumber(name).map(BigDecimal::intValue);
    }

    public boolean has(String name) {
        return byName.containsKey(name);
    }
}
