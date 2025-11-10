package co.edu.uco.parameterservices.catalog.parameter;

import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class ParameterCatalog {

    private static final Logger logger = LoggerFactory.getLogger(ParameterCatalog.class);
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PREFIX = "parameter:";

    // Mapa local de respaldo
    private final Map<String, Parameter> fallbackMemory = new HashMap<>();

    public ParameterCatalog(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;

        fallbackMemory.put("FechaDefectoMaxima", new Parameter("FechaDefectoMaxima", "31/12/2500"));
        fallbackMemory.put("correoAdministrador", new Parameter("correoAdministrador", "admin@uco.edu.co"));
        fallbackMemory.put("numeroMaximoReintentosEnvioCorreo", new Parameter("numeroMaximoReintentosEnvioCorreo", "5"));
    }


    public Parameter getParameter(String key) {
        try {
            Object obj = redisTemplate.opsForValue().get(PREFIX + key);
            if (obj instanceof Parameter) return (Parameter) obj;
        } catch (Exception e) {
            logger.warn("Redis no disponible para key '{}', usando memoria local: {}", key, e.getMessage());
        }
        return fallbackMemory.get(key);
    }

    public void synchronizeParameter(Parameter parameter) {
        try {
            redisTemplate.opsForValue().set(PREFIX + parameter.getKey(), parameter, 10, TimeUnit.MINUTES);
            logger.debug("Parámetro sincronizado en Redis: {}", parameter.getKey());
        } catch (Exception e) {
            logger.warn("No se pudo sincronizar parámetro '{}' en Redis, usando memoria local: {}", 
                    parameter.getKey(), e.getMessage());
        }
        fallbackMemory.put(parameter.getKey(), parameter);
    }

    public void removeParameter(String key) {
        if (redisTemplate != null) {
            redisTemplate.delete(PREFIX + key);
        }
        fallbackMemory.remove(key);
    }

    public Map<String, Parameter> getAllParameters() {
        try {
            Set<String> keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Map<String, Parameter> map = new HashMap<>();
                for (String key : keys) {
                    Object obj = redisTemplate.opsForValue().get(key);
                    if (obj instanceof Parameter) {
                        String paramKey = key.substring(PREFIX.length());
                        map.put(paramKey, (Parameter) obj);
                    }
                }
                if (!map.isEmpty()) {
                    return map;
                }
            }
        } catch (Exception e) {
            logger.warn("Redis no disponible, devolviendo valores locales: {}", e.getMessage());
        }
        return fallbackMemory;
    }

    public void clearAll() {
        if (redisTemplate != null) {
            var keys = redisTemplate.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
        fallbackMemory.clear();
    }
}
