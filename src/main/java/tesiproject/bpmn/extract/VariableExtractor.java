package tesiproject.bpmn.extract;

import org.springframework.stereotype.Component;
import tesiproject.bpmn.model.*;
import tesiproject.bpmn.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class VariableExtractor {

    /*
     Whitelist ruoli umani
     Se vuoi la modalità "prendi tutte le lane", mettila a Set.of() e lascia che il filtro NON_HUMAN_HINTS faccia il grosso
     */
    private static final Set<String> HUMAN_ROLES_INCLUDE = Set.of(
            "secretary", "logisticsDeptHead", "warehouseStaff"
    );

    /*
     Blacklist dei ruoli non umani (usata solo se HUMAN_ROLES_INCLUDE è vuota)
     */
    private static final List<String> NON_HUMAN_HINTS = List.of(
            "system", "digital", "api", "service", "backend", "bot", "robot", "software", "application", "platform",
            "company", "enterprise", "organization"
    );

    public List<Variable> extract(ProcessModel model) {
        List<Variable> vars = new ArrayList<>();

        // Variabili strutturali
        vars.add(Variable.intVar("participantsType", model.getParticipants().size(), VarCategory.STRUCTURAL, VarSource.XML_STRUCTURAL));

        // --- Ruoli umani dalle LANES ---
        int emittedHumanRoles = 0;

        for (Lane lane : model.getLanes()) {
            String role = roleKeyFromLane(lane.getId(), lane.getName());

            if (!isHumanRole(role)) continue;

            // NOTA: value null -> verrà completata dal BenchmarkService via HumanVarsCompleter leggendo defaults.json
            vars.add(new Variable("participants_" + role, VarType.INT, null, null, VarCategory.STRUCTURAL,
                    VarSource.XML_SEMANTIC, null // <-- niente confidence
            ));

            vars.add(new Variable("hours_" + role, VarType.DOUBLE, "H", null, VarCategory.TIME,
                    VarSource.XML_SEMANTIC, null // <-- niente confidence
            ));

            vars.add(new Variable("wageEurPerHour_" + role, VarType.MONEY, "EUR", null,VarCategory.COST,
                    VarSource.XML_SEMANTIC, null // <-- niente confidence
            ));

            emittedHumanRoles++;
        }

        // Conteggio ruoli umani trovati
        vars.add(Variable.intVar("humanRolesCount", emittedHumanRoles, VarCategory.STRUCTURAL, VarSource.XML_SEMANTIC));

        // --- CO2 / carbon ---
        if (model.containsKeyword("carbon") || model.containsKeyword("footprint")) {
            vars.add(Variable.doubleVar("carbonFootprintGCO2", null, "gCO2", VarCategory.KPI, VarSource.XML_SEMANTIC));
        }

        // --- ENERGY / kWh ---
        if (model.containsKeyword("energy") || model.containsKeyword("electric")
                || model.containsKeyword("electricity") || model.containsKeyword("kwh")
                || model.containsKeyword("power") || model.containsKeyword("consumption")) {
            vars.add(Variable.doubleVar("energyConsumptionKWh", null, "kWh", VarCategory.CONSUMPTION, VarSource.XML_SEMANTIC));
        }

        return vars;
    }

    private boolean isHumanRole(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) return false;

        String n = roleKey.toLowerCase(Locale.ROOT);

        // Se whitelist attiva, deve matchare ESATTAMENTE
        if (!HUMAN_ROLES_INCLUDE.isEmpty()) {
            return HUMAN_ROLES_INCLUDE.contains(roleKey);
        }

        // Altrimenti filtra non-umani per hint
        for (String hint : NON_HUMAN_HINTS) {
            if (n.contains(hint)) return false;
        }
        return true;
    }

    /*
     Se laneId è del tipo Lane_<Something>, usa SEMPRE quello altrimenti fallback su laneName
     */
    private String roleKeyFromLane(String laneId, String laneName) {

        // 1) preferisci ID se possibile
        if (laneId != null && laneId.startsWith("Lane_") && laneId.length() > "Lane_".length()) {
            String raw = laneId.substring("Lane_".length()); // es: LogisticsDeptHead
            raw = raw.replaceAll("[^A-Za-z0-9]", "");        // pulizia minima
            if (!raw.isEmpty()) {
                return Character.toLowerCase(raw.charAt(0)) + raw.substring(1); // logisticsDeptHead
            }
        }

        // 2) fallback su name
        String base = (laneName != null && !laneName.isBlank()) ? laneName : laneId;
        if (base == null || base.isBlank()) return "generic";

        base = base.replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (base.isEmpty()) return "generic";

        String[] parts = base.split("\\s+");
        StringBuilder sb = new StringBuilder();

        // lowerCamelCase
        sb.append(parts[0].substring(0, 1).toLowerCase(Locale.ROOT)).append(parts[0].substring(1));
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1));
        }
        return sb.toString();
    }
}
/*
        // --- Probabilità rami per gateway esclusivi ---
        for (Gateway g : model.getGatewaysOfType(GatewayType.EXCLUSIVE)) {
            int out = model.getIndex().getOutgoing(g.getId()).size();
            if (out <= 0) continue;

            String gwKey = safe(g.getName());
            for (int i = 1; i <= out; i++) {
                vars.add(Variable.doubleVar(
                        "p_" + gwKey + "_branch" + i,
                        null,
                        null,
                        VarCategory.PROBABILITY,
                        VarSource.XML_STRUCTURAL
                ));
            }
        }

        // --- Keyword-based (semantica) ---
        if (model.containsKeyword("insurance")) {
            vars.add(Variable.boolVar("insuranceRequired", null, VarCategory.COST, VarSource.XML_SEMANTIC));
            vars.add(Variable.moneyVar("insuranceCostEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
            vars.add(Variable.moneyVar("insuredValueEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
        }

        if (model.containsKeyword("ship") || model.containsKeyword("logistics") || model.containsKeyword("postal")) {
            vars.add(Variable.moneyVar("shippingCostEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
            vars.add(Variable.intVar("deliveryDays", null, VarCategory.TIME, VarSource.XML_SEMANTIC));
        }

        if (model.containsKeyword("packaging") || model.containsKeyword("material")) {
            vars.add(Variable.moneyVar("packagingCostEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
            vars.add(Variable.doubleVar("cardboardKg", null, "KG", VarCategory.CONSUMPTION, VarSource.XML_SEMANTIC));
            vars.add(Variable.doubleVar("bioPlasticKg", null, "KG", VarCategory.CONSUMPTION, VarSource.XML_SEMANTIC));
            vars.add(Variable.doubleVar("paperTapeMeters", null, "M", VarCategory.CONSUMPTION, VarSource.XML_SEMANTIC));
        }

        if (model.containsKeyword("documentation") || model.containsKeyword("label")) {
            vars.add(Variable.moneyVar("documentationCostEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
        }

        if (model.containsKeyword("notify") || model.containsKeyword("notification")) {
            vars.add(Variable.moneyVar("notificationCostEur", null, VarCategory.COST, VarSource.XML_SEMANTIC));
        }
        */