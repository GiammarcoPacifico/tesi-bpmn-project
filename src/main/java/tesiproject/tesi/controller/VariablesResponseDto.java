package tesiproject.tesi.controller;

import java.util.List;

public class VariablesResponseDto {

    private List<VariableDto> variables;
    private String defaultsReport; // Null se non è stato usato alcun default

    public VariablesResponseDto(List<VariableDto> variables, String defaultsReport) {
        this.variables = variables;
        this.defaultsReport = defaultsReport;
    }

    public List<VariableDto> getVariables() {
        return variables;
    }

    public void setVariables(List<VariableDto> variables) {
        this.variables = variables;
    }

    public String getDefaultsReport() {
        return defaultsReport;
    }

    public void setDefaultsReport(String defaultsReport) {
        this.defaultsReport = defaultsReport;
    }
}
