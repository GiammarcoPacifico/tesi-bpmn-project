package tesiproject.tesi.controller;

import java.util.ArrayList;
import java.util.List;

public class DefaultsReport {

    public record Applied(String name, Object value, String unit) {}

    private final List<Applied> applied = new ArrayList<>();

    public void add(Applied a) {
        applied.add(a);
    }

    public boolean isEmpty() {
        return applied.isEmpty();
    }

    public List<Applied> applied() {
        return List.copyOf(applied);
    }

    public String toPrettyString() {
        if (applied.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("----------------------------------------\n");
        sb.append("DEFAULT VARIABLES\n");
        sb.append("----------------------------------------\n");

        for (Applied a : applied) {
            sb.append(a.name())
                    .append(" = ")
                    .append(a.value());

            if (a.unit() != null && !a.unit().isBlank()) {
                sb.append(" ").append(a.unit());
            }
            sb.append("\n");
        }

        sb.append("----------------------------------------\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPrettyString();
    }
}