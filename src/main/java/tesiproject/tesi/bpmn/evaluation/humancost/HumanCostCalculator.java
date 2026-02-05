package tesiproject.tesi.bpmn.evaluation.humancost;

import tesiproject.tesi.bpmn.evaluation.common.VarLookup;
import tesiproject.tesi.bpmn.model.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class HumanCostCalculator {

    public record RoleCosts(
            String role,
            BigDecimal hoursPerOperator,
            BigDecimal wageEurPerHour,
            int participants,
            BigDecimal operatorCostEur,
            BigDecimal operatorsCostEur
    ) {}

    public record Result(
            List<RoleCosts> byRole,
            BigDecimal processCostEur,
            int participantsTotal,
            BigDecimal complexityEurPerOperator
    ) {}

    public Result compute(List<Variable> vars) {
        Set<String> roles = deriveRolesFromParticipantsVars(vars);
        return compute(vars, roles);
    }

    public Result compute(List<Variable> vars, Set<String> roles) {
        VarLookup lk = new VarLookup(vars);

        if (roles == null) roles = Set.of();

        // Nessun ruolo => niente calcolo, niente fallback
        if (roles.isEmpty()) {
            return new Result(
                    List.of(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        List<RoleCosts> roleCosts = new ArrayList<>();

        BigDecimal totalCostEur = BigDecimal.ZERO; // Σ(hours*wage*participants)
        BigDecimal totalHoursH = BigDecimal.ZERO;  // Σ(hours*participants)
        int totalParticipants = 0;

        for (String role : roles) {
            BigDecimal hours = lk.getNumber("hours_" + role).orElse(BigDecimal.ZERO);
            BigDecimal wage  = lk.getNumber("wageEurPerHour_" + role).orElse(BigDecimal.ZERO);
            int participants = lk.getInt("participants_" + role).orElse(0);

            BigDecimal operatorCost = hours.multiply(wage).setScale(2, RoundingMode.HALF_UP);
            BigDecimal operatorsCost = operatorCost
                    .multiply(BigDecimal.valueOf(participants))
                    .setScale(2, RoundingMode.HALF_UP);

            roleCosts.add(new RoleCosts(role, hours, wage, participants, operatorCost, operatorsCost));

            totalCostEur = totalCostEur.add(operatorsCost);
            totalHoursH = totalHoursH.add(hours.multiply(BigDecimal.valueOf(participants)));
            totalParticipants += participants;
        }

        int participantsTotal = lk.getInt("participantsTotal").orElse(totalParticipants);

        BigDecimal complexity;
        if (participantsTotal > 0 && totalHoursH.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costPerHour = totalCostEur.divide(totalHoursH, 6, RoundingMode.HALF_UP);
            complexity = costPerHour.divide(BigDecimal.valueOf(participantsTotal), 2, RoundingMode.HALF_UP);
        } else {
            complexity = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        roleCosts.sort(Comparator.comparing(RoleCosts::role));

        return new Result(
                roleCosts,
                totalCostEur.setScale(2, RoundingMode.HALF_UP),
                participantsTotal,
                complexity
        );
    }

    private Set<String> deriveRolesFromParticipantsVars(List<Variable> vars) {
        if (vars == null) return Set.of();

        return vars.stream()
                .filter(v -> v != null && v.getName() != null && v.getName().startsWith("participants_"))
                .map(v -> v.getName().substring("participants_".length()))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
