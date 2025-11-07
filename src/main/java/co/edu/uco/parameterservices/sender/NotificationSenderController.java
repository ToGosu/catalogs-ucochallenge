package co.edu.uco.parameterservices.sender;

import co.edu.uco.parameterservices.catalog.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/send")
public class NotificationSenderController {

    private final NotificationService notificationService;
    private final EmailSenderService senderService;

    public NotificationSenderController(
            NotificationService notificationService,
            EmailSenderService senderService) {
        this.notificationService = notificationService;
        this.senderService = senderService;
    }

    /**
     * POST /api/v1/send/email
     * Procesa template y envía email
     * 
     * Body:
     * {
     *   "to": "juan@example.com",
     *   "templateCode": "WELCOME_EMAIL",
     *   "variables": {
     *     "nombre": "Juan",
     *     "email": "juan@example.com"
     *   }
     * }
     */
    @PostMapping("/email")
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody SendEmailRequest request) {
        try {
            // 1. Obtener template
            var template = notificationService.findByCode(request.getTemplateCode());
            if (template == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Template no encontrado: " + request.getTemplateCode()));
            }

            // 2. Procesar placeholders
            String processedBody = notificationService.processTemplate(
                request.getTemplateCode(), 
                request.getVariables()
            );

            String processedSubject = template.getSubject();
            if (request.getVariables() != null) {
                for (Map.Entry<String, String> entry : request.getVariables().entrySet()) {
                    processedSubject = processedSubject.replace(
                        "{" + entry.getKey() + "}", 
                        entry.getValue()
                    );
                }
            }

            // 3. Enviar email
            senderService.sendEmail(
                request.getTo(),
                processedSubject,
                processedBody,
                template.getFormat()
            );

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Email enviado correctamente a " + request.getTo()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error enviando email: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/send/sms
     * Procesa template y envía SMS
     */
    @PostMapping("/sms")
    public ResponseEntity<Map<String, String>> sendSms(@RequestBody SendSmsRequest request) {
        try {
            // 1. Procesar template
            String processedMessage = notificationService.processTemplate(
                request.getTemplateCode(),
                request.getVariables()
            );

            // 2. Enviar SMS
            senderService.sendSms(request.getTo(), processedMessage);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "SMS enviado correctamente a " + request.getTo()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Error enviando SMS: " + e.getMessage()));
        }
    }

    // DTOs
    public static class SendEmailRequest {
        private String to;
        private String templateCode;
        private Map<String, String> variables;

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public Map<String, String> getVariables() { return variables; }
        public void setVariables(Map<String, String> variables) { this.variables = variables; }
    }

    public static class SendSmsRequest {
        private String to;
        private String templateCode;
        private Map<String, String> variables;

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public Map<String, String> getVariables() { return variables; }
        public void setVariables(Map<String, String> variables) { this.variables = variables; }
    }
}
