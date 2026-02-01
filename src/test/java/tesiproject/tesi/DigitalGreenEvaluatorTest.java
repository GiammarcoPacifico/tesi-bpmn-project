package tesiproject.tesi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tesiproject.tesi.bpmn.evaluation.DigitalGreenEvaluator;
import tesiproject.tesi.bpmn.evaluation.humancost.HumanCostCalculator;
import tesiproject.tesi.bpmn.evaluation.humancost.HumanVarsCompleter;
import tesiproject.tesi.bpmn.evaluation.resourcescost.ResourceCostCalculator;
import tesiproject.tesi.bpmn.extract.VariableExtractor;
import tesiproject.tesi.bpmn.model.*;
import tesiproject.tesi.bpmn.parser.BpmnXmlParser;
import tesiproject.tesi.controller.DefaultsCatalogProvider;
import tesiproject.tesi.controller.DefaultsReport;
import tesiproject.tesi.service.BenchmarkService;

import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DigitalGreenEvaluatorTest {

    @Test
    void evaluate() {
        // --- Parse BPMN ---
        InputStream is = getClass().getResourceAsStream("/bpmn/dispatch_goods.bpmn");
        assertNotNull(is, "Non trovo /bpmn/dispatch_goods.bpmn in src/test/resources");

        BpmnXmlParser parser = new BpmnXmlParser();
        ProcessModel model = parser.parse(is);

        // --- Extract vars ---
        VariableExtractor extractor = new VariableExtractor();
        List<Variable> vars = extractor.extract(model);

        // --- Benchmark (da JSON) ---
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultsCatalogProvider catalogProvider = new DefaultsCatalogProvider(objectMapper);

        // Crea HumanVarsCompleter e lo passa al BenchmarkService
        HumanVarsCompleter humanVarsCompleter = new HumanVarsCompleter(catalogProvider);
        BenchmarkService benchmarkService = new BenchmarkService(catalogProvider, humanVarsCompleter);

        DefaultsReport defaultsReport = benchmarkService.assignDefaults(model, vars);

        // ===== DEBUG: input variabili principali =====
        System.out.println("\n----------------------------------------");
        System.out.println("INPUT VARIABLES");
        System.out.println("----------------------------------------");

        if (defaultsReport != null && !defaultsReport.isEmpty()) {
            System.out.print(defaultsReport.toPrettyString());
            System.out.println();
        }

        System.out.println("--- participants_* ---");
        vars.stream()
                .filter(v -> v != null && v.getName() != null && v.getName().startsWith("participants_"))
                .sorted(Comparator.comparing(Variable::getName))
                .forEach(v -> System.out.println(v.getName() + " = " + v.getValue()));

        System.out.println("\n--- hours_* ---");
        vars.stream()
                .filter(v -> v != null && v.getName() != null && v.getName().startsWith("hours_"))
                .sorted(Comparator.comparing(Variable::getName))
                .forEach(v -> System.out.println(v.getName() + " = " + v.getValue() + " " + unit(v)));

        System.out.println("\n--- wageEurPerHour_* ---");
        vars.stream()
                .filter(v -> v != null && v.getName() != null && v.getName().startsWith("wageEurPerHour_"))
                .sorted(Comparator.comparing(Variable::getName))
                .forEach(v -> System.out.println(v.getName() + " = " + v.getValue() + " " + unit(v)));

        System.out.println("\n--- energy / co2 ---");
        vars.stream()
                .filter(v -> v != null && v.getName() != null)
                .filter(v -> v.getName().equals("energyConsumptionKWh") || v.getName().equals("carbonFootprintGCO2"))
                .sorted(Comparator.comparing(Variable::getName))
                .forEach(v -> System.out.println(
                        v.getName() + " = " + v.getValue() + " " + unit(v) + " | src=" + v.getSource()
                ));

        // --- CALCOLO HUMAN COST ---
        HumanCostCalculator humanCalc = new HumanCostCalculator();
        HumanCostCalculator.Result humanResult = humanCalc.compute(vars);

        System.out.println("\n----------------------------------------");
        System.out.println("HUMAN COST BREAKDOWN");
        System.out.println("----------------------------------------");

        for (HumanCostCalculator.RoleCosts rc : humanResult.byRole()) {
            System.out.printf(
                    "%s -> participants=%d, hours=%s, wage=%s, operatorCost=%s, totalCost=%s%n",
                    rc.role(),
                    rc.participants(),
                    rc.hoursPerOperator(),
                    rc.wageEurPerHour(),
                    rc.operatorCostEur(),
                    rc.operatorsCostEur()
            );
        }

        System.out.println("\nHuman process cost EUR: " + humanResult.processCostEur());
        System.out.println("Human participants total: " + humanResult.participantsTotal());
        System.out.println("Human complexity: " + humanResult.complexityEurPerOperator());

        // --- CALCOLO RESOURCE COST ---
        ResourceCostCalculator resourceCalc = new ResourceCostCalculator();
        ResourceCostCalculator.Result resourceResult = resourceCalc.compute(vars);

        System.out.println("\n----------------------------------------");
        System.out.println("RESOURCE COST BREAKDOWN");
        System.out.println("----------------------------------------");

        System.out.println("--- Resource steps ---");
        System.out.println("co2 (G_CO2)               = " + resourceResult.co2G());
        System.out.println("kWh from CO2              = " + resourceResult.kwhFromCo2());
        System.out.println("kWh from BPMN (if present)= " + resourceResult.kwhFromBpmn());
        System.out.println("kWh total                 = " + resourceResult.kwhTotal());
        System.out.println("Total cost EUR            = " + resourceResult.costTotalEur());
        System.out.println("Resource usage            = " + resourceResult.resourcesUsage());

        // --- CALCOLO DGF ---
        DigitalGreenEvaluator evaluator = new DigitalGreenEvaluator();
        DigitalGreenEvaluator.Result evalResult =
                evaluator.evaluate(vars, humanCalc, resourceCalc);

        System.out.println("\n----------------------------------------");
        System.out.println("DIGITAL GREEN FUNCTION");
        System.out.println("----------------------------------------");
        System.out.println("Human complexity               = " + evalResult.humanComplexity());
        System.out.println("Resource usage                 = " + evalResult.resourceUsage());
        System.out.println("DGF = complexity/resourceUsage = " + evalResult.digitalGreenFunction());
        System.out.println("----------------------------------------");

        // --- ASSERT MINIMI (sanity) ---
        assertTrue(
                humanResult.byRole().stream().anyMatch(r -> r.role().equals("logisticsDeptHead")),
                "logisticsDeptHead deve essere incluso nel calcolo human cost"
        );

        assertTrue(humanResult.processCostEur().signum() > 0, "Il costo umano totale deve essere > 0");
        assertTrue(evalResult.resourceUsage().signum() >= 0, "resourceUsage deve essere >= 0");
        assertTrue(evalResult.digitalGreenFunction().signum() >= 0, "DGF deve essere >= 0");
    }

    private static String unit(Variable v) {
        return v.getUnit() == null ? "" : v.getUnit();
    }
}
