package co.edu.uco.parameterservices.catalog.notification;

import co.edu.uco.parameterservices.catalog.notification.domain.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para NotificationCatalog")
class NotificationCatalogTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private NotificationCatalog catalog;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        catalog = new NotificationCatalog(redisTemplate);
    }

    @Test
    @DisplayName("Debería retornar template de bienvenida por email")
    void shouldReturnWelcomeEmailTemplate() {
        // Act
        NotificationTemplate template = catalog.getTemplate("WELCOME_EMAIL");

        // Assert
        assertNotNull(template);
        assertEquals("WELCOME_EMAIL", template.getCode());
        assertEquals("EMAIL", template.getType());
        assertNotNull(template.getSubject());
        assertTrue(template.getBody().contains("{nombre}"));
    }

    @Test
    @DisplayName("Debería procesar placeholders correctamente")
    void shouldProcessPlaceholdersCorrectly() {
        // Arrange
        Map<String, String> variables = Map.of(
            "nombre", "Juan",
            "email", "juan@example.com"
        );

        // Act
        String processed = catalog.processTemplate("WELCOME_EMAIL", variables);

        // Assert
        assertNotNull(processed);
        assertTrue(processed.contains("Juan"));
        assertTrue(processed.contains("juan@example.com"));
        assertFalse(processed.contains("{nombre}"));
    }

    @Test
    @DisplayName("Debería retornar todos los templates")
    void shouldReturnAllTemplates() {
        // Act
        Map<String, NotificationTemplate> templates = catalog.getAllTemplates();

        // Assert
        assertNotNull(templates);
        assertTrue(templates.size() >= 5);
        assertTrue(templates.containsKey("WELCOME_EMAIL"));
        assertTrue(templates.containsKey("WELCOME_SMS"));
    }

    @Test
    @DisplayName("Debería sincronizar template en fallback si Redis falla")
    void shouldSynchronizeInFallbackWhenRedisFails() {
        // Arrange
        NotificationTemplate template = new NotificationTemplate(
            "CUSTOM_EMAIL",
            "EMAIL",
            "Test Subject",
            "Test Body",
            "HTML"
        );

        doThrow(new RuntimeException("Redis down"))
            .when(valueOperations).set(anyString(), any(), any(), any());

        // Act
        catalog.synchronizeTemplate(template);

        // Assert
        NotificationTemplate retrieved = catalog.getTemplate("CUSTOM_EMAIL");
        assertNotNull(retrieved);
        assertEquals("CUSTOM_EMAIL", retrieved.getCode());
    }
}
