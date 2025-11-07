package co.edu.uco.parameterservices.catalog.notification.service;

import co.edu.uco.parameterservices.catalog.notification.NotificationCatalog;
import co.edu.uco.parameterservices.catalog.notification.domain.NotificationTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationService {

    private final NotificationCatalog catalog;

    public NotificationService(NotificationCatalog catalog) {
        this.catalog = catalog;
    }

    public NotificationTemplate findByCode(String code) {
        return catalog.getTemplate(code);
    }

    public Map<String, NotificationTemplate> findAll() {
        return catalog.getAllTemplates();
    }

    public Map<String, NotificationTemplate> findByType(String type) {
        return catalog.getAllTemplates().entrySet().stream()
            .filter(entry -> type.equalsIgnoreCase(entry.getValue().getType()))
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    public String processTemplate(String code, Map<String, String> variables) {
        return catalog.processTemplate(code, variables);
    }

    public void synchronize(NotificationTemplate template) {
        catalog.synchronizeTemplate(template);
    }
}
