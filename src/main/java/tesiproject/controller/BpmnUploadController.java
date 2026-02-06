package tesiproject.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tesiproject.controller.dto.VariablesResponseDto;
import tesiproject.service.BpmnIngestService;

import java.io.InputStream;

@RestController
@RequestMapping("/api/bpmn")
public class BpmnUploadController {

    private final BpmnIngestService ingestService;

    public BpmnUploadController(BpmnIngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping(value = "/variables", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VariablesResponseDto uploadAndExtract(@RequestParam("file") InputStream xml) {
        return ingestService.extractVariables(xml);
    }
}
