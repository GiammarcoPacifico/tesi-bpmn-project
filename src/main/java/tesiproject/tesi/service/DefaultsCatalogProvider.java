package tesiproject.tesi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tesiproject.tesi.controller.dto.DefaultsCatalog;

import java.io.InputStream;

@Component
public class DefaultsCatalogProvider {

    private final ObjectMapper objectMapper;
    private volatile DefaultsCatalog cached;

    public DefaultsCatalogProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DefaultsCatalog get() {
        DefaultsCatalog local = cached;
        if (local != null) return local;

        synchronized (this) {
            if (cached != null) return cached;
            cached = loadFromClasspath("json/defaults.json");
            return cached;
        }
    }

    private DefaultsCatalog loadFromClasspath(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, DefaultsCatalog.class);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load defaults catalog from classpath: " + path, e);
        }
    }
}
