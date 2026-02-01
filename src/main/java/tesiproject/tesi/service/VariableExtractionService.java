package tesiproject.tesi.service;

import org.springframework.stereotype.Service;
import tesiproject.tesi.bpmn.extract.VariableExtractor;
import tesiproject.tesi.bpmn.model.ProcessModel;
import tesiproject.tesi.bpmn.model.Variable;

import java.util.List;

@Service
public class VariableExtractionService {

    private final VariableExtractor extractor;

    public VariableExtractionService(VariableExtractor extractor) {
        this.extractor = extractor;
    }

    public List<Variable> extract(ProcessModel model) {
        return extractor.extract(model);
    }
}
