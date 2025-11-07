package co.edu.uco.parameterservices.catalog.parameter;

import co.edu.uco.parameterservices.catalog.parameter.domain.Parameter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class ParameterCatalog {

    private static RedisTemplate<String, Object> redisTemplate;
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
            System.out.println("Redis no disponible, usando memoria local.");
        }
        return fallbackMemory.get(key);
    }

    public void synchronizeParameter(Parameter parameter) {
        try {
            redisTemplate.opsForValue().set(PREFIX + parameter.getKey(), parameter, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.out.println("No se pudo sincronizar en Redis, usando memoria local.");
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
            Map<Object, Object> redisValues = redisTemplate.opsForHash().entries(PREFIX);
            if (redisValues != null && !redisValues.isEmpty()) {
                Map<String, Parameter> map = new HashMap<>();
                redisValues.forEach((k, v) -> map.put(k.toString(), (Parameter) v));
                return map;
            }
        } catch (Exception e) {
            System.out.println("Redis no disponible, devolviendo valores locales.");
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
