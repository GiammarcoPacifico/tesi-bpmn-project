package tesiproject;

import org.junit.jupiter.api.Test;
import tesiproject.bpmn.extract.VariableExtractor;
import tesiproject.bpmn.model.ProcessModel;
import tesiproject.bpmn.model.Variable;
import tesiproject.bpmn.model.*;
import tesiproject.bpmn.parser.BpmnXmlParser;

import java.io.InputStream;
import java.util.List;

public class SanityTest {

    private final BpmnXmlParser parser = new BpmnXmlParser();
    private final VariableExtractor extractor = new VariableExtractor();

    @Test
    void printExtractedVariableNames_onlyFromBpmn() throws Exception {

        try (InputStream xml = getClass().getResourceAsStream("/bpmn/dispatch_goods.bpmn")) {
            if (xml == null) {
                throw new RuntimeException("BPMN file not found in test resources: /bpmn/dispatch_goods.bpmn");
            }

            // Parse BPMN
            ProcessModel model = parser.parse(xml);

            // Extract variables (SOLO BPMN, nessun default JSON)
            List<Variable> vars = extractor.extract(model);

            // Stampa SOLO i nomi delle variabili
            vars.stream().map(Variable::getName).filter(name -> name != null && !name.isBlank())
                    .sorted(String.CASE_INSENSITIVE_ORDER).forEach(System.out::println);
        }
    }
}
