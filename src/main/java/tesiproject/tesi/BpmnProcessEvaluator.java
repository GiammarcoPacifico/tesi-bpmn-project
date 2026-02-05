package tesiproject.tesi;

import com.fasterxml.jackson.databind.ObjectMapper;
import tesiproject.tesi.bpmn.evaluation.DigitalGreenEvaluator;
import tesiproject.tesi.bpmn.evaluation.humancost.HumanCostCalculator;
import tesiproject.tesi.bpmn.evaluation.humancost.HumanVarsCompleter;
import tesiproject.tesi.bpmn.evaluation.resourcescost.ResourceCostCalculator;
import tesiproject.tesi.bpmn.extract.VariableExtractor;
import tesiproject.tesi.bpmn.model.*;
import tesiproject.tesi.bpmn.parser.BpmnXmlParser;
import tesiproject.tesi.service.DefaultsCatalogProvider;
import tesiproject.tesi.controller.dto.DefaultsReport;
import tesiproject.tesi.service.BenchmarkService;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class BpmnProcessEvaluator {

    public static void main(String[] args) throws Exception {

        // CARICAMENTO BPMN
        String classpathBpmn = "/bpmn/dispatch_goods.bpmn";
        String fileBpmnPath  = "src/main/resources/bpmn/dispatch_goods.bpmn";

        try (InputStream is = openBpmnInputStream(classpathBpmn, fileBpmnPath)) {

            // PARSE
            BpmnXmlParser parser = new BpmnXmlParser();
            ProcessModel model = parser.parse(is);

            // ESTRAZIONE VARIABILI
            VariableExtractor extractor = new VariableExtractor();
            List<Variable> vars = extractor.extract(model);

            // BENCHMARK (JSON defaults)
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultsCatalogProvider catalogProvider = new DefaultsCatalogProvider(objectMapper);

            HumanVarsCompleter humanVarsCompleter = new HumanVarsCompleter(catalogProvider);
            BenchmarkService benchmarkService = new BenchmarkService(catalogProvider, humanVarsCompleter);

            DefaultsReport report = benchmarkService.assignDefaults(model, vars);

            // DEBUG INPUT
            System.out.println("\n==============================");
            System.out.println("=== DEFAULTS APPLIED (FROM JSON) ===");
            System.out.println("==============================");
            if (report != null && !report.isEmpty()) {
                System.out.print(report.toPrettyString());
            } else {
                System.out.println("(no defaults applied from JSON)");
            }

            System.out.println("\n==============================");
            System.out.println("=== VARIABLES (AFTER BENCHMARK) ===");
            System.out.println("==============================");

            vars.stream()
                    .filter(v -> v != null && v.getName() != null)
                    .sorted(Comparator.comparing(Variable::getName))
                    .forEach(v -> System.out.println(v.toHumanString()));

            // CALCOLO COSTI UMANI
            HumanCostCalculator humanCalc = new HumanCostCalculator();
            HumanCostCalculator.Result humanResult = humanCalc.compute(vars);

            System.out.println("\n==============================");
            System.out.println("=== HUMAN COST BREAKDOWN ===");
            System.out.println("==============================");

            BigDecimal totalWorkedHours = BigDecimal.ZERO;

            for (HumanCostCalculator.RoleCosts rc : humanResult.byRole()) {

                BigDecimal roleHours =
                        rc.hoursPerOperator().multiply(BigDecimal.valueOf(rc.participants()));

                totalWorkedHours = totalWorkedHours.add(roleHours);

                System.out.printf(
                        "%s -> participants=%d | hours/operator=%s | TOTAL HOURS=%s | wage=%s | role cost=%s%n",
                        rc.role(),
                        rc.participants(),
                        rc.hoursPerOperator(),
                        roleHours,
                        rc.wageEurPerHour(),
                        rc.operatorsCostEur()
                );
            }

            System.out.println("\n--- HUMAN TOTALS ---");
            System.out.println("Total human cost EUR       = " + humanResult.processCostEur());
            System.out.println("Total participants         = " + humanResult.participantsTotal());
            System.out.println("TOTAL WORKED HOURS (ALL)   = " + totalWorkedHours);
            System.out.println("Human complexity           = " + humanResult.complexityEurPerOperator());

            // CALCOLO COSTI RISORSE
            ResourceCostCalculator resourceCalc = new ResourceCostCalculator();
            ResourceCostCalculator.Result resourceResult = resourceCalc.compute(vars);

            System.out.println("\n==============================");
            System.out.println("=== RESOURCE COST BREAKDOWN ===");
            System.out.println("==============================");

            System.out.println("CO2 (g)                   = " + resourceResult.co2G());
            System.out.println("kWh from CO2              = " + resourceResult.kwhFromCo2());
            System.out.println("kWh from BPMN/default     = " + resourceResult.kwhFromBpmn());
            System.out.println("kWh TOTAL                 = " + resourceResult.kwhTotal());
            System.out.println("Energy cost EUR           = " + resourceResult.costTotalEur());
            System.out.println("Resource usage            = " + resourceResult.resourcesUsage());

            // DIGITAL GREEN FUNCTION
            DigitalGreenEvaluator evaluator = new DigitalGreenEvaluator();
            DigitalGreenEvaluator.Result dgf = evaluator.evaluate(vars, humanCalc, resourceCalc);

            System.out.println("\n==============================");
            System.out.println("=== DIGITAL GREEN FUNCTION ===");
            System.out.println("==============================");

            System.out.println("Human complexity   = " + dgf.humanComplexity());
            System.out.println("Resource usage     = " + dgf.resourceUsage());
            System.out.println("DGF                = " + dgf.digitalGreenFunction());
        }
    }

    private static InputStream openBpmnInputStream(String classpathPath, String fileFallbackPath) throws Exception {
        InputStream cp = BpmnProcessEvaluator.class.getResourceAsStream(classpathPath);
        if (cp != null) return cp;

        // Fallback su file system
        return new FileInputStream(fileFallbackPath);
    }
}
