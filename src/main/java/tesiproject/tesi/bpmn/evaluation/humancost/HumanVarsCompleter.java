package tesiproject.tesi.bpmn.evaluation.humancost;

import org.springframework.stereotype.Component;
import tesiproject.tesi.bpmn.model.VarCategory;
import tesiproject.tesi.bpmn.model.VarSource;
import tesiproject.tesi.bpmn.model.VarType;
import tesiproject.tesi.bpmn.model.Variable;
import tesiproject.tesi.controller.DefaultsCatalog;
import tesiproject.tesi.controller.DefaultsCatalogProvider;
import tesiproject.tesi.controller.DefaultsReport;

import java.math.BigDecimal;
import java.util.*;

@Component
public class HumanVarsCompleter {

    private final DefaultsCatalogProvider catalogProvider;

    public HumanVarsCompleter(DefaultsCatalogProvider catalogProvider) {
        this.catalogProvider = Objects.requireNonNull(catalogProvider);
    }

    /*
     Se trova un ruolo, garantisce che esistano participants_/hours_/wageEurPerHour_ per quel ruolo.
     Se la variabile esiste ma value==null -> la completa.
     Se la variabile NON esiste -> la crea.
     I valori arrivano SOLO da defaults.json (roleDefaults / fallbacks).
     */
    public void ensureRoleDefaults(List<Variable> vars, DefaultsReport report) {
        if (vars == null || vars.isEmpty()) return;

        DefaultsCatalog catalog = catalogProvider.get();

        Set<String> roles = extractRolesFromVars(vars);
        if (roles.isEmpty()) return;

        for (String role : roles) {
            DefaultsCatalog.RoleDefaultEntry entry =
                    (catalog.roleDefaults != null) ? catalog.roleDefaults.get(role) : null;

            Integer participants = firstNonNull(
                    entry != null ? entry.participants : null,
                    catalog.fallbacks != null ? catalog.fallbacks.participants : null,
                    1
            );

            BigDecimal hoursBd = asBigDecimal(firstNonNull(
                    entry != null ? entry.hours : null,
                    catalog.fallbacks != null ? catalog.fallbacks.hours : null,
                    "0"
            ));
            Double hours = hoursBd.doubleValue(); // Coerente con VarType.DOUBLE

            BigDecimal wage = asBigDecimal(firstNonNull(
                    entry != null ? entry.wageEurPerHour : null,
                    catalog.fallbacks != null ? catalog.fallbacks.wageEurPerHour : null,
                    "0"
            ));

            ensureVar(vars, report,
                    "participants_" + role,
                    VarType.INT,
                    null,
                    participants,
                    VarCategory.STRUCTURAL
            );

            ensureVar(vars, report,
                    "hours_" + role,
                    VarType.DOUBLE,
                    "H",
                    hours,
                    VarCategory.TIME
            );

            ensureVar(vars, report,
                    "wageEurPerHour_" + role,
                    VarType.MONEY,
                    "EUR",
                    wage,
                    VarCategory.COST
            );
        }
    }

    private Set<String> extractRolesFromVars(List<Variable> vars) {
        Set<String> roles = new LinkedHashSet<>();
        for (Variable v : vars) {
            if (v == null) continue;
            String n = v.getName();
            if (n == null) continue;

            String role = null;
            if (n.startsWith("participants_") && n.length() > "participants_".length()) {
                role = n.substring("participants_".length()).trim();
            } else if (n.startsWith("hours_") && n.length() > "hours_".length()) {
                role = n.substring("hours_".length()).trim();
            } else if (n.startsWith("wageEurPerHour_") && n.length() > "wageEurPerHour_".length()) {
                role = n.substring("wageEurPerHour_".length()).trim();
            }

            if (role != null && !role.isBlank()) roles.add(role);
        }
        return roles;
    }

    private void ensureVar(List<Variable> vars,
                           DefaultsReport report,
                           String name,
                           VarType type,
                           String unit,
                           Object defaultValue,
                           VarCategory category) {

        Variable v = findByName(vars, name);

        // Se non esiste -> crea
        if (v == null) {
            Variable created = new Variable(
                    name,
                    type,
                    unit,
                    defaultValue,
                    category,
                    VarSource.BENCHMARK_DEFAULT,
                    null
            );
            vars.add(created);

            if (report != null) report.add(new DefaultsReport.Applied(name, defaultValue, unit));
            return;
        }

        // Se esiste ma value null -> completa
        if (v.getValue() == null) {
            v.setType(type);
            v.setValue(defaultValue);
            if ((v.getUnit() == null || v.getUnit().isBlank()) && unit != null) v.setUnit(unit);
            v.setSource(VarSource.BENCHMARK_DEFAULT);

            if (report != null) report.add(new DefaultsReport.Applied(name, defaultValue, v.getUnit()));
        }
    }

    private Variable findByName(List<Variable> vars, String name) {
        for (Variable v : vars) {
            if (v != null && name.equals(v.getName())) return v;
        }
        return null;
    }

    private static <T> T firstNonNull(T a, T b, T c) {
        if (a != null) return a;
        if (b != null) return b;
        return c;
    }

    private BigDecimal asBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(o));
    }
}