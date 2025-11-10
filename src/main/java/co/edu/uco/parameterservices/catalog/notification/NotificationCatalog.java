package co.edu.uco.parameterservices.catalog.notification;

import co.edu.uco.parameterservices.catalog.notification.domain.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationCatalog {

    private static final Logger logger = LoggerFactory.getLogger(NotificationCatalog.class);
    private static final String PREFIX = "notification:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, NotificationTemplate> fallbackMemory = new HashMap<>();

    public NotificationCatalog(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        initializeDefaultTemplates();
    }

    /**
     * Inicializa templates por defecto en memoria
     */
    private void initializeDefaultTemplates() {
        // Template de bienvenida por email
        fallbackMemory.put("WELCOME_EMAIL", new NotificationTemplate(
            "WELCOME_EMAIL",
            "EMAIL",
            "Bienvenido a UcoChallenge - {nombre}",
            "<html><body>" +
            "<h1>¡Bienvenido {nombre}!</h1>" +
            "<p>Gracias por registrarte en UcoChallenge.</p>" +
            "<p>Tu cuenta ha sido creada exitosamente.</p>" +
            "<p>Email: {email}</p>" +
            "<hr>" +
            "<p>Equipo UcoChallenge</p>" +
            "</body></html>",
            "HTML"
        ));

        // Template de bienvenida por SMS
        fallbackMemory.put("WELCOME_SMS", new NotificationTemplate(
            "WELCOME_SMS",
            "SMS",
            null, // SMS no tiene asunto
            "Hola {nombre}, bienvenido a UcoChallenge. Tu cuenta ha sido creada exitosamente.",
            "PLAIN_TEXT"
        ));

        // Template de confirmación de email
        fallbackMemory.put("EMAIL_CONFIRMATION", new NotificationTemplate(
            "EMAIL_CONFIRMATION",
            "EMAIL",
            "Confirma tu correo electrónico",
            "<html><body>" +
            "<h2>Confirmación de Email</h2>" +
            "<p>Hola {nombre},</p>" +
            "<p>Para confirmar tu correo electrónico, haz clic en el siguiente enlace:</p>" +
            "<a href='{confirmationLink}'>Confirmar Email</a>" +
            "<p>Si no solicitaste esta confirmación, ignora este mensaje.</p>" +
            "</body></html>",
            "HTML"
        ));

        // Template de recuperación de contraseña
        fallbackMemory.put("PASSWORD_RESET", new NotificationTemplate(
            "PASSWORD_RESET",
            "EMAIL",
            "Recuperación de Contraseña - UcoChallenge",
            "<html><body>" +
            "<h2>Recuperación de Contraseña</h2>" +
            "<p>Hola {nombre},</p>" +
            "<p>Recibimos una solicitud para restablecer tu contraseña.</p>" +
            "<p>Tu código de verificación es: <strong>{code}</strong></p>" +
            "<p>Este código expira en 15 minutos.</p>" +
            "<p>Si no solicitaste esto, ignora este correo.</p>" +
            "</body></html>",
            "HTML"
        ));

        // Template de notificación de error
        fallbackMemory.put("ERROR_NOTIFICATION", new NotificationTemplate(
            "ERROR_NOTIFICATION",
            "EMAIL",
            "Alerta del Sistema - UcoChallenge",
            "<html><body>" +
            "<h2 style='color:red;'>Notificación de Error</h2>" +
            "<p>Se ha detectado un error en el sistema:</p>" +
            "<ul>" +
            "<li>Error: {errorMessage}</li>" +
            "<li>Fecha: {timestamp}</li>" +
            "<li>Usuario: {userId}</li>" +
            "</ul>" +
            "</body></html>",
            "HTML"
        ));
    }

    /**
     * Obtiene un template de notificación por código
     */
    public NotificationTemplate getTemplate(String code) {
        try {
            Object cached = redisTemplate.opsForValue().get(PREFIX + code);
            if (cached instanceof NotificationTemplate) {
                return (NotificationTemplate) cached;
            }
        } catch (Exception e) {
            logger.warn("Redis no disponible para template '{}', usando fallback local: {}", code, e.getMessage());
        }
        return fallbackMemory.get(code);
    }

    /**
     * Obtiene todos los templates
     */
    public Map<String, NotificationTemplate> getAllTemplates() {
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Map<String, NotificationTemplate> map = new HashMap<>();
                for (String key : keys) {
                    Object obj = redisTemplate.opsForValue().get(key);
                    if (obj instanceof NotificationTemplate) {
                        String templateCode = key.substring(PREFIX.length());
                        map.put(templateCode, (NotificationTemplate) obj);
                    }
                }
                if (!map.isEmpty()) {
                    return map;
                }
            }
        } catch (Exception e) {
            logger.warn("Redis no disponible, devolviendo templates locales: {}", e.getMessage());
        }
        return fallbackMemory;
    }

    /**
     * Sincroniza un template en Redis
     */
    public void synchronizeTemplate(NotificationTemplate template) {
        try {
            redisTemplate.opsForValue().set(
                PREFIX + template.getCode(), 
                template, 
                30, 
                TimeUnit.MINUTES
            );
            logger.debug("Template sincronizado en Redis: {}", template.getCode());
        } catch (Exception e) {
            logger.warn("No se pudo sincronizar template '{}' en Redis: {}", template.getCode(), e.getMessage());
        }
        fallbackMemory.put(template.getCode(), template);
    }

    /**
     * Elimina un template
     */
    public void removeTemplate(String code) {
        try {
            redisTemplate.delete(PREFIX + code);
            logger.debug("Template eliminado de Redis: {}", code);
        } catch (Exception e) {
            logger.warn("Error eliminando template '{}' de Redis: {}", code, e.getMessage());
        }
        fallbackMemory.remove(code);
    }

    /**
     * Limpia todos los templates
     */
    public void clearAll() {
        try {
            var keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.warn("Error limpiando Redis: {}", e.getMessage());
        }
        fallbackMemory.clear();
    }

    /**
     * Procesa placeholders en el template
     * Ej: "Hola {nombre}" + {"nombre": "Juan"} -> "Hola Juan"
     */
    public String processTemplate(String templateCode, Map<String, String> variables) {
        NotificationTemplate template = getTemplate(templateCode);
        if (template == null) {
            return null;
        }

        String processed = template.getBody();
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                processed = processed.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return processed;
    }
}
