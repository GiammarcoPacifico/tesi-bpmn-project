package tesiproject.bpmn.evaluation;

import tesiproject.bpmn.evaluation.humancost.HumanCostCalculator;
import tesiproject.bpmn.evaluation.resourcescost.ResourceCostCalculator;
import tesiproject.bpmn.model.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DigitalGreenEvaluator {

    public record Result(
            BigDecimal humanComplexity,     // EUR / operatore
            BigDecimal resourceUsage,       // uso risorse normalizzato
            BigDecimal digitalGreenFunction // DGF finale
    ) {}

    /*
     DGF = complexity / resourceUsage
     */
    public Result evaluate(
            List<Variable> vars,
            HumanCostCalculator humanCostCalculator,
            ResourceCostCalculator resourceCostCalculator
    ) {

        // 1) Calcoli parziali
        HumanCostCalculator.Result humanResult =
                humanCostCalculator.compute(vars);

        ResourceCostCalculator.Result resourceResult =
                resourceCostCalculator.compute(vars);

        BigDecimal complexity = humanResult.complexityEurPerOperator();
        BigDecimal resourceUsage = resourceResult.resourcesUsage();

        // 2) Protezione divisione per zero
        BigDecimal dgf = BigDecimal.ZERO;
        if (resourceUsage.compareTo(BigDecimal.ZERO) > 0) {
            dgf = complexity.divide(resourceUsage, 6, RoundingMode.HALF_UP);
        }

        return new Result(
                complexity.setScale(2, RoundingMode.HALF_UP),
                resourceUsage.setScale(2, RoundingMode.HALF_UP),
                dgf.setScale(6, RoundingMode.HALF_UP)
        );
    }
}
