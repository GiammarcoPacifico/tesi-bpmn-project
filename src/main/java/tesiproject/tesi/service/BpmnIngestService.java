package tesiproject.tesi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tesiproject.tesi.bpmn.model.ProcessModel;
import tesiproject.tesi.bpmn.model.Variable;
import tesiproject.tesi.bpmn.parser.BpmnXmlParser;
import tesiproject.tesi.controller.DefaultsReport;
import tesiproject.tesi.controller.VariableDto;
import tesiproject.tesi.controller.VariablesResponseDto;

import java.io.InputStream;
import java.util.List;

@Service
public class BpmnIngestService {

    private final BpmnXmlParser parser;
    private final VariableExtractionService variableExtractionService;
    private final BenchmarkService benchmarkService;

    public BpmnIngestService(BpmnXmlParser parser,
                             VariableExtractionService variableExtractionService,
                             BenchmarkService benchmarkService) {
        this.parser = parser;
        this.variableExtractionService = variableExtractionService;
        this.benchmarkService = benchmarkService;
    }

    public VariablesResponseDto extractVariables(InputStream xml) {
        ProcessModel model = parser.parse(xml);

        // 1) Variabili estratte dal BPMN
        List<Variable> vars = variableExtractionService.extract(model);

        // 2) Assegna valori stimati (benchmark / default) + report
        DefaultsReport report = benchmarkService.assignDefaults(model, vars);

        // 3) DTO per output
        List<VariableDto> out = vars.stream().map(VariableDto::from).toList();

        // 4) Wrapper con report (null se non usati default)
        String defaultsReportText = null;
        if (report != null && !report.isEmpty()) {
            defaultsReportText = report.toPrettyString();
        }

        return new VariablesResponseDto(out, defaultsReportText);
    }
}
