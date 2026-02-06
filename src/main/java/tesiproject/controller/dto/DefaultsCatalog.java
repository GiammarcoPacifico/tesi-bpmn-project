package tesiproject.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultsCatalog {

    public Map<String, GlobalDefaultEntry> globalDefaults;
    public Map<String, RoleDefaultEntry> roleDefaults;
    public Fallbacks fallbacks;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GlobalDefaultEntry {
        public String type;   // "DOUBLE", "MONEY", "INT", "BOOLEAN"
        public String unit;   // "kWh", "EUR", ecc.
        public Object value;  // numero/bool/string
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoleDefaultEntry {
        public Integer participants;
        public String hours;            // BigDecimal string
        public String wageEurPerHour;   // BigDecimal string
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fallbacks {
        public Integer participants;
        public String hours;
        public String wageEurPerHour;
    }
}
