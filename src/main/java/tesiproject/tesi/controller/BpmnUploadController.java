package tesiproject.tesi.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tesiproject.tesi.service.BpmnIngestService;

import java.io.InputStream;
import java.util.List;

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
