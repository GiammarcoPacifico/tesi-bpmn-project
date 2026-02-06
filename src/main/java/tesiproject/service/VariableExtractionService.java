package tesiproject.service;

import org.springframework.stereotype.Service;
import tesiproject.bpmn.extract.VariableExtractor;
import tesiproject.bpmn.model.ProcessModel;
import tesiproject.bpmn.model.Variable;

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
