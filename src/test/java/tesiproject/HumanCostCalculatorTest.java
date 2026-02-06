package tesiproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tesiproject.bpmn.evaluation.humancost.HumanCostCalculator;
import tesiproject.bpmn.evaluation.humancost.HumanVarsCompleter;
import tesiproject.bpmn.extract.VariableExtractor;
import tesiproject.bpmn.model.ProcessModel;
import tesiproject.bpmn.model.Variable;
import tesiproject.bpmn.model.*;
import tesiproject.bpmn.parser.BpmnXmlParser;
import tesiproject.service.DefaultsCatalogProvider;
import tesiproject.controller.dto.DefaultsReport;
import tesiproject.service.BenchmarkService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HumanCostCalculatorTest {

    @Test
    void compute_shouldCalculateCostsAndComplexity_withAllStepsPrinted() throws Exception {

        // --- Parse BPMN ---
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bpmn/dispatch_goods.bpmn")) {
            assertNotNull(is, "File non trovato: src/test/resources/bpmn/dispatch_goods.bpmn");

            BpmnXmlParser parser = new BpmnXmlParser();
            ProcessModel model = parser.parse(is);

            // --- Extract vars ---
            VariableExtractor extractor = new VariableExtractor();
            List<Variable> vars = extractor.extract(model);

            // --- Benchmark (JSON defaults) ---
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultsCatalogProvider catalogProvider = new DefaultsCatalogProvider(objectMapper);

            HumanVarsCompleter humanVarsCompleter = new HumanVarsCompleter(catalogProvider);

            BenchmarkService benchmarkService = new BenchmarkService(catalogProvider, humanVarsCompleter);

            DefaultsReport report = benchmarkService.assignDefaults(model, vars);

            // === REPORT: variabili prese dal JSON ===
            if (report != null && !report.isEmpty()) {
                System.out.print(report.toPrettyString()); // include già separatori
            } else {
                System.out.println("(no defaults applied from JSON)");
            }

            // === DEBUG: variabili umane participants/hours/wage ===
            System.out.println("\n----------------------------------------");
            System.out.println("HUMAN INPUTS (participants/hours/wage)");
            System.out.println("----------------------------------------");

            vars.stream().filter(v -> v != null && v.getName() != null).filter(v -> v.getName().startsWith("participants_")
                            || v.getName().startsWith("hours_") || v.getName().startsWith("wageEurPerHour_"))
                    .sorted(Comparator.comparing(Variable::getName)).forEach(v -> System.out.println(
                            v.getName() + " = " + v.getValue() + (v.getUnit() != null ? " " + v.getUnit() : "")));

            // --- CALCOLO ---
            HumanCostCalculator calculator = new HumanCostCalculator();
            HumanCostCalculator.Result result = calculator.compute(vars);

            // === OUTPUT RISULTATI + spiegazione formule ===
            System.out.println("\n----------------------------------------");
            System.out.println("HUMAN COST BREAKDOWN");
            System.out.println("----------------------------------------");

            for (HumanCostCalculator.RoleCosts rc : result.byRole()) {

                BigDecimal operatorCost = rc.hoursPerOperator().multiply(rc.wageEurPerHour());
                BigDecimal operatorsCost = operatorCost.multiply(BigDecimal.valueOf(rc.participants()));

                System.out.println("\nROLE: " + rc.role());
                System.out.println("  participants            = " + rc.participants());
                System.out.println("  hoursPerOperator        = " + rc.hoursPerOperator());
                System.out.println("  wageEurPerHour          = " + rc.wageEurPerHour());

                System.out.println("  operatorCost = h*w      = " + rc.hoursPerOperator() + " * " + rc.wageEurPerHour()
                        + " = " + operatorCost.setScale(2, RoundingMode.HALF_UP));

                System.out.println("  totalRoleCost = op*P    = " + operatorCost.setScale(2, RoundingMode.HALF_UP)
                        + " * " + rc.participants()
                        + " = " + operatorsCost.setScale(2, RoundingMode.HALF_UP));

                System.out.println("  (calculator) operatorCostEur   = " + rc.operatorCostEur());
                System.out.println("  (calculator) operatorsCostEur  = " + rc.operatorsCostEur());
            }

            System.out.println("\n----------------------------------------");
            System.out.println("FINAL RESULTS");
            System.out.println("----------------------------------------");
            System.out.println("Process cost EUR          = " + result.processCostEur());
            System.out.println("Participants total        = " + result.participantsTotal());
            System.out.println("Complexity EUR/operator   = " + result.complexityEurPerOperator());
            System.out.println("(formula) complexity = (processCost / totalHours) / participantsTotal");
            System.out.println("----------------------------------------");

            // --- ASSERT MINIMI ---
            assertTrue(result.byRole().stream().anyMatch(r -> "logisticsDeptHead".equals(r.role())),
                    "logisticsDeptHead deve essere incluso nel calcolo");
            assertTrue(result.processCostEur().compareTo(BigDecimal.ZERO) > 0,
                    "processCostEur deve essere > 0");
        }
    }
}