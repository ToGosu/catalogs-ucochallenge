package co.edu.uco.parameterservices.catalog.notification.controller;

import co.edu.uco.parameterservices.catalog.notification.domain.NotificationTemplate;
import co.edu.uco.parameterservices.catalog.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    /**
     * GET /api/v1/notifications
     * Obtiene todos los templates de notificaciones
     */
    @GetMapping
    public ResponseEntity<Map<String, NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * GET /api/v1/notifications/{code}
     * Obtiene un template específico por código
     */
    @GetMapping("/{code}")
    public ResponseEntity<NotificationTemplate> getTemplateByCode(
            @jakarta.validation.constraints.NotBlank @PathVariable String code) {
        NotificationTemplate template = service.findByCode(code);
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(template);
    }

    /**
     * GET /api/v1/notifications/type/{type}
     * Obtiene templates por tipo (EMAIL o SMS)
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, NotificationTemplate>> getTemplatesByType(
            @jakarta.validation.constraints.NotBlank @PathVariable String type) {
        return ResponseEntity.ok(service.findByType(type));
    }

    /**
     * POST /api/v1/notifications/process
     * Procesa un template con variables
     * Body: {
     *   "templateCode": "WELCOME_EMAIL",
     *   "variables": {
     *     "nombre": "Juan",
     *     "email": "juan@example.com"
     *   }
     * }
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, String>> processTemplate(
            @jakarta.validation.Valid @RequestBody ProcessTemplateRequest request) {
        
        String processed = service.processTemplate(
            request.getTemplateCode(), 
            request.getVariables()
        );
        
        if (processed == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(Map.of(
            "templateCode", request.getTemplateCode(),
            "processedBody", processed
        ));
    }

    /**
     * POST /api/v1/notifications
     * Crea o actualiza un template
     */
    @PostMapping
    public ResponseEntity<NotificationTemplate> createOrUpdateTemplate(
            @jakarta.validation.Valid @RequestBody NotificationTemplate template) {
        
        service.synchronize(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    // DTO interno para el request de procesamiento
    public static class ProcessTemplateRequest {
        @jakarta.validation.constraints.NotBlank(message = "El código del template es requerido")
        private String templateCode;
        
        private Map<String, String> variables;

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public Map<String, String> getVariables() {
            return variables;
        }

        public void setVariables(Map<String, String> variables) {
            this.variables = variables;
        }
    }
}
