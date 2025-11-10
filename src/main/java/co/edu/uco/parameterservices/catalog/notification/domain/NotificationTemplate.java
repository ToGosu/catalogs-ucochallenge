package co.edu.uco.parameterservices.catalog.notification.domain;

import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Representa un template de notificación (email o SMS)
 * Contiene asunto, cuerpo y formato
 */
public class NotificationTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "El código del template es requerido")
    private String code;           // Código único del template (ej: "WELCOME_EMAIL")
    
    @NotBlank(message = "El tipo es requerido")
    @Pattern(regexp = "EMAIL|SMS", message = "El tipo debe ser EMAIL o SMS")
    private String type;           // EMAIL | SMS
    
    private String subject;        // Asunto (para emails)
    
    @NotBlank(message = "El cuerpo del mensaje es requerido")
    private String body;           // Cuerpo del mensaje (puede tener placeholders)
    
    @Pattern(regexp = "HTML|PLAIN_TEXT", message = "El formato debe ser HTML o PLAIN_TEXT")
    private String format;         // HTML | PLAIN_TEXT
    
    public NotificationTemplate() {
    }

    public NotificationTemplate(String code, String type, String subject, String body, String format) {
        this.code = code;
        this.type = type;
        this.subject = subject;
        this.body = body;
        this.format = format;
    }

    // Getters y Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "NotificationTemplate{" +
                "code='" + code + '\'' +
                ", type='" + type + '\'' +
                ", subject='" + subject + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
