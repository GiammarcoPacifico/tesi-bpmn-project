package tesiproject.tesi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import tesiproject.tesi.bpmn.evaluation.humancost.HumanVarsCompleter;
import tesiproject.tesi.bpmn.evaluation.resourcescost.ResourceCostCalculator;
import tesiproject.tesi.bpmn.extract.VariableExtractor;
import tesiproject.tesi.bpmn.model.*;
import tesiproject.tesi.bpmn.parser.BpmnXmlParser;
import tesiproject.tesi.controller.DefaultsCatalogProvider;
import tesiproject.tesi.controller.DefaultsReport;
import tesiproject.tesi.service.BenchmarkService;

import java.io.InputStream;
import java.util.List;

public class ResourceCostCalculatorTest {

    @Test
    void resourceCosts_pipeline_isShown_parse_extract_defaults_calculate() throws Exception {

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("bpmn/dispatch_goods.bpmn")) {
            if (is == null) {
                throw new IllegalStateException("File non trovato: src/test/resources/bpmn/dispatch_goods.bpmn");
            }

            // Pipeline: parse -> extract
            BpmnXmlParser parser = new BpmnXmlParser();
            ProcessModel model = parser.parse(is);

            VariableExtractor extractor = new VariableExtractor();
            List<Variable> vars = extractor.extract(model);

            // Stampa: SOLO i nomi trovati nel BPMN
            System.out.println("----------------------------------------");
            System.out.println("VARIABLES FOUND IN BPMN (NAMES ONLY)");
            System.out.println("----------------------------------------");
            vars.stream().filter(v -> v != null && v.getName() != null).map(Variable::getName).distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER).forEach(System.out::println);
            System.out.println("----------------------------------------");
            System.out.println();

            // Benchmark defaults (JSON)
            ObjectMapper objectMapper = new ObjectMapper();
            DefaultsCatalogProvider catalogProvider = new DefaultsCatalogProvider(objectMapper);

            HumanVarsCompleter humanCompleter = new HumanVarsCompleter(catalogProvider);

            BenchmarkService benchmark = new BenchmarkService(catalogProvider, humanCompleter);
            DefaultsReport report = benchmark.assignDefaults(model, vars);

            // Report: SOLO variabili prese dal JSON
            if (report != null && !report.isEmpty()) {
                System.out.print(report.toPrettyString());
                System.out.println();
            }

            // Calcolo resource-cost
            ResourceCostCalculator calc = new ResourceCostCalculator();
            ResourceCostCalculator.Result r = calc.compute(vars);

            // Stampa risultati calcolo
            System.out.println("----------------------------------------");
            System.out.println("RESOURCE COST CALCULATION");
            System.out.println("----------------------------------------");
            System.out.println("co2 (G_CO2)                = " + r.co2G());
            System.out.println("kwhFromCo2 (=co2*0.00385)   = " + r.kwhFromCo2());
            System.out.println("kwhFromBpmn (if present)    = " + r.kwhFromBpmn());
            System.out.println("kwhTotal                    = " + r.kwhTotal());
            System.out.println("costTotalEur (=kwh*0.12762) = " + r.costTotalEur());
            System.out.println("resourcesUsage (*0.5203)    = " + r.resourcesUsage());
            System.out.println("----------------------------------------");
        }
    }
}
