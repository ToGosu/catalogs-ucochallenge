# Plantillas para Implementación de WAF, OpenTelemetry y HTTPS

Este directorio contiene plantillas reutilizables para implementar WAF (Nginx + ModSecurity), OpenTelemetry y HTTPS en los servicios API Gateway y Catalog Service.

## Archivos de Plantilla

### 1. `spring-boot-otel-config.yml`
Configuración de OpenTelemetry para `application.yml` de servicios Spring Boot.

**Uso:**
- Copiar el contenido a `src/main/resources/application.yml` de cada servicio
- Ajustar `SERVICE_NAME` según el servicio (api-gateway o catalog-service)

### 2. `nginx-service-config.conf`
Configuración de location blocks para Nginx.

**Uso:**
- Agregar las secciones `location` correspondientes a `nginx/default.conf`
- Descomentar y ajustar según el servicio

### 3. `docker-compose-service-template.yml`
Template de servicio Docker Compose con configuración OpenTelemetry.

**Uso:**
- Copiar el bloque de servicio al `docker-compose.yml` principal
- Ajustar nombres de contenedores, puertos y variables de entorno
- Asegurar que el servicio esté en la misma red (`app-network`)

### 4. `Dockerfile-template`
Template de Dockerfile con OpenTelemetry Java Agent.

**Uso:**
- Copiar y renombrar a `Dockerfile` en cada servicio
- Ajustar:
  - `OTEL_SERVICE_NAME`: nombre del servicio
  - `EXPOSE`: puerto del servicio (8090 para API Gateway, 8082 para Catalog Service)
  - `HEALTHCHECK`: puerto en la URL de health check

## Pasos de Implementación

### Para API Gateway:

1. **Dockerfile:**
   ```bash
   cp templates/Dockerfile-template api-gateway/Dockerfile
   # Editar: OTEL_SERVICE_NAME=api-gateway, EXPOSE 8090
   ```

2. **application.yml:**
   ```bash
   # Agregar contenido de templates/spring-boot-otel-config.yml
   # Cambiar SERVICE_NAME: api-gateway
   ```

3. **docker-compose.yml:**
   ```bash
   # Agregar servicio api-gateway desde templates/docker-compose-service-template.yml
   # Ajustar puerto a 8090
   ```

4. **nginx/default.conf:**
   ```bash
   # Agregar location /api-gateway/ desde templates/nginx-service-config.conf
   ```

### Para Catalog Service:

1. **Dockerfile:**
   ```bash
   cp templates/Dockerfile-template catalog-service/Dockerfile
   # Editar: OTEL_SERVICE_NAME=catalog-service, EXPOSE 8082
   ```

2. **application.yml:**
   ```bash
   # Agregar contenido de templates/spring-boot-otel-config.yml
   # Cambiar SERVICE_NAME: catalog-service
   ```

3. **docker-compose.yml:**
   ```bash
   # Agregar servicio catalog-service desde templates/docker-compose-service-template.yml
   # Ajustar puerto a 8082
   ```

4. **nginx/default.conf:**
   ```bash
   # Agregar location /catalog/ desde templates/nginx-service-config.conf
   ```

## Certificados SSL

Los certificados ya están generados en el directorio `certs/` del proyecto principal. Los servicios pueden usar los mismos certificados o generar nuevos con el script `scripts/generate-certs.sh`.

## Verificación

Después de implementar:

1. **Verificar HTTPS:**
   ```bash
   curl -k https://localhost:8443/api-gateway/health
   curl -k https://localhost:8443/catalog/health
   ```

2. **Verificar OpenTelemetry:**
   - Abrir Jaeger UI: http://localhost:16686
   - Buscar traces del servicio correspondiente

3. **Verificar WAF:**
   - Revisar logs de ModSecurity: `docker logs nginx-waf`
   - Probar ataque SQL injection: `curl -k "https://localhost:8443/api-gateway/test?id=1' OR '1'='1"`

## Notas Importantes

- Todos los servicios deben estar en la misma red Docker (`app-network`)
- El OTEL Collector debe estar disponible antes de iniciar los servicios
- Los certificados autofirmados solo son para desarrollo
- ModSecurity está en modo detección (no bloquea, solo registra)

