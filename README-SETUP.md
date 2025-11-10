# Configuración de WAF, OpenTelemetry y HTTPS para Catalog Service

Este documento describe la configuración implementada para el Catalog Service con:
- **WAF (Web Application Firewall)**: Nginx como reverse proxy
- **OpenTelemetry**: Instrumentación para observabilidad y tracing
- **HTTPS**: Protocolo seguro con certificados SSL/TLS

## Estructura del Proyecto

```
catalogservice/
├── Dockerfile                    # Dockerfile con OpenTelemetry agent
├── docker-compose.yml            # Orquestación de servicios
├── otel-collector-config.yaml    # Configuración del OpenTelemetry Collector
├── nginx/
│   ├── nginx.conf               # Configuración principal de Nginx
│   ├── default.conf             # Configuración del servidor virtual
│   └── modsecurity/
│       └── modsecurity.conf     # Configuración de ModSecurity
├── certs/                       # Certificados SSL (generar con scripts)
├── scripts/
│   ├── generate-certs.sh        # Script para Linux/Mac
│   └── generate-certs.ps1       # Script para Windows
└── src/main/resources/
    └── application.yml          # Configuración Spring Boot con OpenTelemetry
```

## Prerrequisitos

1. **Docker** y **Docker Compose** instalados
2. **OpenSSL** (para generar certificados) o usar PowerShell en Windows
3. **Java 21** (para desarrollo local)

## Pasos de Instalación

### 1. Generar Certificados SSL

**En Windows (PowerShell):**
```powershell
.\scripts\generate-certs.ps1
```

**En Linux/Mac:**
```bash
chmod +x scripts/generate-certs.sh
./scripts/generate-certs.sh
```

Los certificados se generarán en el directorio `certs/`:
- `server.crt`: Certificado público
- `server.key`: Clave privada

> **Nota**: Estos son certificados autofirmados para desarrollo. En producción, usa certificados de una CA confiable.

### 2. Construir y Levantar los Servicios

```bash
docker-compose up -d --build
```

Este comando:
- Construye la imagen del catalog-service con OpenTelemetry
- Levanta Redis para cache
- Levanta OpenTelemetry Collector
- Levanta Jaeger para visualización de traces
- Levanta Nginx con WAF y HTTPS

### 3. Verificar que los Servicios Estén Corriendo

```bash
docker-compose ps
```

Todos los servicios deben estar en estado "Up".

## Verificación

### 1. Verificar HTTPS

```bash
# Verificar health check
curl -k https://localhost:8443/health

# Verificar endpoint del servicio
curl -k https://localhost:8443/catalog/actuator/health
```

> **Nota**: El flag `-k` ignora la verificación del certificado autofirmado.

### 2. Verificar OpenTelemetry

1. Abrir Jaeger UI en el navegador: http://localhost:16686
2. Seleccionar el servicio `catalog-service` en el dropdown
3. Hacer clic en "Find Traces"
4. Deberías ver los traces de las peticiones

### 3. Verificar WAF (Nginx)

```bash
# Ver logs de Nginx
docker logs nginx-waf

# Probar una petición
curl -k https://localhost:8443/catalog/api-docs
```

### 4. Verificar el Servicio

```bash
# Health check directo al servicio
curl http://localhost:8082/actuator/health

# A través de Nginx (HTTPS)
curl -k https://localhost:8443/catalog/actuator/health
```

## Configuración de Servicios

### Catalog Service

- **Puerto interno**: 8082
- **OpenTelemetry**: Configurado para enviar traces a `otel-collector:4317`
- **Redis**: Conectado a `redis:6379`
- **Health Check**: `/actuator/health`

### Nginx (WAF)

- **HTTP**: Puerto 8080 (redirige a HTTPS)
- **HTTPS**: Puerto 8443
- **Rutas**:
  - `/catalog/` → Proxy a catalog-service:8082
  - `/catalog/actuator/` → Proxy a catalog-service:8082/actuator/
  - `/health` → Health check de Nginx

### OpenTelemetry Collector

- **OTLP gRPC**: Puerto 4317
- **OTLP HTTP**: Puerto 4318
- **Exporta a**: Jaeger en puerto 14250

### Jaeger

- **UI**: http://localhost:16686
- **gRPC**: Puerto 14250

### Redis

- **Puerto**: 6379
- **Uso**: Cache del servicio

## Desarrollo Local

Para desarrollo local sin Docker:

1. Asegúrate de que Redis esté corriendo:
   ```bash
   redis-server
   ```

2. Actualiza `application.yml` para usar `localhost` en lugar de nombres de contenedores

3. Ejecuta la aplicación:
   ```bash
   ./mvnw spring-boot:run
   ```

## Troubleshooting

### Los certificados no se generan

- Verifica que OpenSSL esté instalado: `openssl version`
- En Windows, el script PowerShell puede usar certificados de Windows si OpenSSL no está disponible

### Nginx no inicia

- Verifica que los certificados existan en `certs/`
- Revisa los logs: `docker logs nginx-waf`
- Verifica la sintaxis de configuración: `docker exec nginx-waf nginx -t`

### OpenTelemetry no muestra traces

- Verifica que el collector esté corriendo: `docker logs otel-collector`
- Verifica la conexión: `docker exec catalog-service wget -O- http://otel-collector:4317`
- Revisa los logs del servicio: `docker logs catalog-service`

### El servicio no se conecta a Redis

- Verifica que Redis esté corriendo: `docker logs redis`
- Verifica la conexión: `docker exec catalog-service ping redis`

## Notas Importantes

1. **ModSecurity**: La configuración de ModSecurity está comentada porque requiere una imagen de Nginx con ModSecurity compilado. Para habilitarlo, usa una imagen como `owasp/modsecurity-crs:nginx-alpine` o compila Nginx con ModSecurity.

2. **Certificados**: Los certificados autofirmados solo son para desarrollo. En producción, usa certificados de Let's Encrypt o una CA confiable.

3. **Red Docker**: Todos los servicios están en la red `app-network` para comunicación interna.

4. **Health Checks**: Los servicios tienen health checks configurados para reinicio automático en caso de fallos.

## Próximos Pasos

1. Configurar ModSecurity con reglas personalizadas
2. Implementar rate limiting en Nginx
3. Configurar alertas basadas en métricas de OpenTelemetry
4. Implementar certificados de producción con Let's Encrypt

