package tesiproject.tesi.bpmn.evaluation.resourcescost;

import tesiproject.tesi.bpmn.evaluation.humancost.VarLookup;
import tesiproject.tesi.bpmn.model.Variable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ResourceCostCalculator {

    public record Result(
            BigDecimal co2G,              // carbonFootprintGCO2
            BigDecimal kwhFromCo2,        // co2 * 0.00385
            BigDecimal kwhFromBpmn,       // energyConsumptionKWh
            BigDecimal kwhTotal,          // somma
            BigDecimal costTotalEur,      // kwhTotal * 0.12762
            BigDecimal resourcesUsage     // costTotalEur * 0.5203
    ) {}

    public Result compute(List<Variable> vars) {
        VarLookup lk = new VarLookup(vars);

        // 1) input
        BigDecimal co2G = lk.getNumber("carbonFootprintGCO2").orElse(BigDecimal.ZERO);
        BigDecimal kwhFromBpmn = lk.getNumber("energyConsumptionKWh").orElse(BigDecimal.ZERO);

        // 2) co2 -> kwh
        BigDecimal kwhFromCo2 = co2G.multiply(ResourceCostConstants.KWH_PER_G_CO2);

        // 3) kwh totali
        BigDecimal kwhTotal = kwhFromCo2.add(kwhFromBpmn);

        // 4) costo totale energia
        BigDecimal costTotalEur = kwhTotal.multiply(ResourceCostConstants.COST_EUR_PER_KWH);

        // 5) uso risorse normalizzato
        BigDecimal resourcesUsage = costTotalEur.multiply(ResourceCostConstants.NORMALIZATION);

        // Scale finali: kWh e grammi più “precisi”, i costi hanno 2 cifre decimali
        co2G = co2G.setScale(6, RoundingMode.HALF_UP);
        kwhFromCo2 = kwhFromCo2.setScale(6, RoundingMode.HALF_UP);
        kwhFromBpmn = kwhFromBpmn.setScale(6, RoundingMode.HALF_UP);
        kwhTotal = kwhTotal.setScale(6, RoundingMode.HALF_UP);
        costTotalEur = costTotalEur.setScale(2, RoundingMode.HALF_UP);
        resourcesUsage = resourcesUsage.setScale(2, RoundingMode.HALF_UP);

        return new Result(co2G, kwhFromCo2, kwhFromBpmn, kwhTotal, costTotalEur, resourcesUsage);
    }
}
