package tesiproject.service;

import org.springframework.stereotype.Service;
import tesiproject.bpmn.evaluation.humancost.HumanVarsCompleter;
import tesiproject.bpmn.model.*;
import tesiproject.bpmn.model.*;
import tesiproject.controller.dto.DefaultsCatalog;
import tesiproject.controller.dto.DefaultsReport;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BenchmarkService {

    private final DefaultsCatalogProvider catalogProvider;
    private final HumanVarsCompleter humanVarsCompleter;

    public BenchmarkService(DefaultsCatalogProvider catalogProvider,
                            HumanVarsCompleter humanVarsCompleter) {
        this.catalogProvider = Objects.requireNonNull(catalogProvider);
        this.humanVarsCompleter = Objects.requireNonNull(humanVarsCompleter);
    }

    public DefaultsReport assignDefaults(ProcessModel model, List<Variable> vars) {

        DefaultsCatalog catalog = catalogProvider.get();
        DefaultsReport report = new DefaultsReport();

        if (vars == null || vars.isEmpty()) return report;

        // Umane (dal JSON) + report
        humanVarsCompleter.ensureRoleDefaults(vars, report);

        // ParticipantsCount = somma di participants_* (DERIVATA -> niente report)
        int totalParticipants = 0;
        for (Variable v : vars) {
            if (v == null || v.getName() == null) continue;
            if (!v.getName().startsWith("participants_")) continue;

            Object val = v.getValue();
            Integer pi = null;

            if (val instanceof Number n) pi = n.intValue();
            else if (val != null) {
                try { pi = Integer.parseInt(String.valueOf(val)); } catch (Exception ignored) {}
            }

            if (pi != null) totalParticipants += pi;
        }

        upsertParticipantsCount(vars, totalParticipants);

        /* FORZA presenza dei kWh e grammi di CO2:
        - se energyConsumptionKWh NON c'è tra le vars -> creala dal JSON
        - se c'è ma value==null -> completa dal JSON
        - se c'è e value!=null -> NON toccare (vince BPMN)
         */
        ensureGlobalDefaultPresent(vars, report, catalog, "energyConsumptionKWh", VarCategory.CONSUMPTION);
        ensureGlobalDefaultPresent(vars, report, catalog, "carbonFootprintGCO2", VarCategory.KPI);

        // Default globali (JSON), solo se value==null e non XML_STRUCTURAL
        if (catalog.globalDefaults != null && !catalog.globalDefaults.isEmpty()) {
            for (Variable v : vars) {
                if (v == null || v.getName() == null) continue;
                if (v.getValue() != null) continue;
                if (v.getSource() == VarSource.XML_STRUCTURAL) continue;

                DefaultsCatalog.GlobalDefaultEntry entry = catalog.globalDefaults.get(v.getName());
                if (entry == null) continue;

                applyGlobalEntry(v, entry);

                // Report: preso dal JSON
                report.add(new DefaultsReport.Applied(v.getName(), v.getValue(), v.getUnit()));
            }
        }

        return report;
    }

    /*
     Garantisce che una variabile "globale" del catalogo esista in vars e venga valorizzata dal JSON (solo se manca o se ha value==null)
     Se invece ha già value!=null, non fa nulla.
     */
    private void ensureGlobalDefaultPresent(List<Variable> vars, DefaultsReport report, DefaultsCatalog catalog, String varName, VarCategory categoryIfCreated) {
        if (catalog == null || catalog.globalDefaults == null) return;

        DefaultsCatalog.GlobalDefaultEntry entry = catalog.globalDefaults.get(varName);
        if (entry == null) return;

        Variable v = findByName(vars, varName);

        // Se NON esiste -> crea + applica default
        if (v == null) {
            Variable created = new Variable(
                    varName,
                    parseVarType(entry.type),
                    entry.unit,
                    null, // valorizzato sotto con applyGlobalEntry()
                    categoryIfCreated,
                    VarSource.BENCHMARK_DEFAULT,
                    null
            );
            vars.add(created);

            applyGlobalEntry(created, entry);

            // Report: preso dal JSON
            if (report != null) {
                report.add(new DefaultsReport.Applied(created.getName(), created.getValue(), created.getUnit()));
            }
            return;
        }

        // Se esiste e ha già valore -> vince BPMN
        if (v.getValue() != null) return;

        // Se esiste ma value==null -> completa da JSON
        applyGlobalEntry(v, entry);

        if (report != null) {
            report.add(new DefaultsReport.Applied(v.getName(), v.getValue(), v.getUnit()));
        }
    }

    private VarType parseVarType(String raw) {
        if (raw == null) return VarType.DOUBLE;
        String t = raw.trim().toUpperCase();
        return switch (t) {
            case "INT" -> VarType.INT;
            case "BOOL", "BOOLEAN" -> VarType.BOOLEAN;
            case "MONEY" -> VarType.MONEY;
            case "DOUBLE" -> VarType.DOUBLE;
            default -> VarType.DOUBLE;
        };
    }

    // DERIVATA -> niente report
    private void upsertParticipantsCount(List<Variable> vars, int totalParticipants) {
        Variable pc = findByName(vars, "participantsCount");
        if (pc == null) {
            vars.add(new Variable(
                    "participantsCount",
                    VarType.INT,
                    null,
                    totalParticipants,
                    VarCategory.STRUCTURAL,
                    VarSource.BENCHMARK_DEFAULT,
                    null
            ));
        } else {
            pc.setType(VarType.INT);
            pc.setValue(totalParticipants);
            pc.setSource(VarSource.BENCHMARK_DEFAULT);
        }
    }

    private Variable findByName(List<Variable> vars, String name) {
        for (Variable v : vars) {
            if (v != null && name.equals(v.getName())) return v;
        }
        return null;
    }

    private void applyGlobalEntry(Variable v, DefaultsCatalog.GlobalDefaultEntry entry) {
        if ((v.getUnit() == null || v.getUnit().isBlank()) && entry.unit != null) {
            v.setUnit(entry.unit);
        }

        String t = entry.type != null ? entry.type.trim().toUpperCase() : "DOUBLE";
        switch (t) {
            case "INT" -> setInt(v, asInt(entry.value));
            case "DOUBLE" -> setDouble(v, asDouble(entry.value));
            case "BOOL", "BOOLEAN" -> setBool(v, asBool(entry.value));
            case "MONEY" -> setMoney(v, asBigDecimal(entry.value), v.getUnit() != null ? v.getUnit() : "EUR");
            default -> setDouble(v, asDouble(entry.value));
        }
    }

    private void setInt(Variable v, int value) {
        v.setType(VarType.INT);
        v.setValue(value);
        v.setSource(VarSource.BENCHMARK_DEFAULT);
    }

    private void setDouble(Variable v, double value) {
        v.setType(VarType.DOUBLE);
        v.setValue(value);
        v.setSource(VarSource.BENCHMARK_DEFAULT);
    }

    private void setBool(Variable v, boolean value) {
        v.setType(VarType.BOOLEAN);
        v.setValue(value);
        v.setSource(VarSource.BENCHMARK_DEFAULT);
    }

    private void setMoney(Variable v, BigDecimal amount, String unit) {
        v.setType(VarType.MONEY);
        v.setValue(amount);
        if (v.getUnit() == null || v.getUnit().isBlank()) v.setUnit(unit);
        v.setSource(VarSource.BENCHMARK_DEFAULT);
    }

    private int asInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    private double asDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Double d) return d;
        if (o instanceof BigDecimal bd) return bd.doubleValue();
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(o));
    }

    private boolean asBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(o));
    }

    private BigDecimal asBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Integer i) return BigDecimal.valueOf(i.longValue());
        if (o instanceof Long l) return BigDecimal.valueOf(l);
        if (o instanceof Double d) return BigDecimal.valueOf(d);
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(o));
    }
}
