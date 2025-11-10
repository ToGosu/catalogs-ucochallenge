package co.edu.uco.parameterservices.catalog.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import co.edu.uco.parameterservices.catalog.message.domain.Message;

@Component
public class MessageCatalog {

    private static final Logger logger = LoggerFactory.getLogger(MessageCatalog.class);
    private static final String PREFIX = "message:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Message> fallbackMemory = new HashMap<>();

    public MessageCatalog(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Carga inicial de fallback local
        fallbackMemory.put("MSG_OK", new Message("MSG_OK", "INFO", "Operación exitosa"));
        fallbackMemory.put("MSG_ERR", new Message("MSG_ERR", "ERROR", "Ocurrió un error inesperado"));
        fallbackMemory.put("USR_001", new Message("USR_001", "SUCCESS", "Usuario registrado correctamente"));
        fallbackMemory.put("USR_002", new Message("USR_002", "ERROR", "El usuario ya existe en el sistema"));
    }

    public Message getMessage(String code) {
        try {
            Object cached = redisTemplate.opsForValue().get(PREFIX + code);
            if (cached instanceof Message) {
                return (Message) cached;
            }
        } catch (Exception e) {
            logger.warn("Redis no disponible para código '{}', usando fallback local: {}", code, e.getMessage());
        }
        return fallbackMemory.get(code);
    }

    public Map<String, Message> getAllMessages() {
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Map<String, Message> map = new HashMap<>();
                for (String key : keys) {
                    Object obj = redisTemplate.opsForValue().get(key);
                    if (obj instanceof Message) {
                        String messageCode = key.substring(PREFIX.length());
                        map.put(messageCode, (Message) obj);
                    }
                }
                if (!map.isEmpty()) {
                    return map;
                }
            }
        } catch (Exception e) {
            logger.warn("Redis no disponible, devolviendo fallback local: {}", e.getMessage());
        }
        return fallbackMemory;
    }

    public void synchronizeMessage(Message message) {
        try {
            redisTemplate.opsForValue().set(PREFIX + message.getCode(), message, 10, TimeUnit.MINUTES);
            logger.debug("Mensaje sincronizado en Redis: {}", message.getCode());
        } catch (Exception e) {
            logger.warn("No se pudo sincronizar mensaje '{}' en Redis: {}", message.getCode(), e.getMessage());
        }
        fallbackMemory.put(message.getCode(), message);
    }

    public void clearAll() {
        try {
            var keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            logger.warn("Error limpiando Redis, limpiando solo fallback local: {}", e.getMessage());
        }
        fallbackMemory.clear();
    }
}
