# Guía de Implementación CI/CD para API Gateway y Catalog Service

Esta guía proporciona instrucciones paso a paso para implementar un pipeline de CI/CD completo con análisis estático de código, validaciones de seguridad y pruebas automatizadas en los proyectos **API Gateway** y **Catalog Service**.

---

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Estructura del Pipeline CI/CD](#estructura-del-pipeline-cicd)
3. [Configuración para API Gateway](#configuración-para-api-gateway)
4. [Configuración para Catalog Service](#configuración-para-catalog-service)
5. [Herramientas de Análisis Estático](#herramientas-de-análisis-estático)
6. [Configuración de Seguridad](#configuración-de-seguridad)
7. [Pruebas Automatizadas](#pruebas-automatizadas)
8. [Despliegue Continuo](#despliegue-continuo)
9. [Troubleshooting](#troubleshooting)

---

## 🔧 Requisitos Previos

### Herramientas Necesarias

1. **GitHub/GitLab** (repositorio de código)
2. **Maven 3.6+** (gestión de dependencias)
3. **JDK 21** (versión de Java)
4. **Docker** (para contenedores)
5. **Cuentas de servicios** (opcionales pero recomendadas):
   - SonarQube Cloud o servidor propio
   - Snyk (análisis de vulnerabilidades)
   - Codecov (cobertura de código)

### Estructura de Proyectos

Asegúrate de que tus proyectos tengan la siguiente estructura:

```
api-gateway/
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── src/
│   ├── main/
│   └── test/
├── pom.xml
├── checkstyle.xml
└── Dockerfile

catalog-service/
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── src/
│   ├── main/
│   └── test/
├── pom.xml
├── checkstyle.xml
└── Dockerfile
```

---

## 🚀 Estructura del Pipeline CI/CD

El pipeline debe incluir las siguientes etapas:

1. **Checkout** - Obtener código del repositorio
2. **Build** - Compilar el proyecto
3. **Test** - Ejecutar pruebas unitarias e integración
4. **Static Analysis** - Análisis estático de código
5. **Security Scan** - Análisis de vulnerabilidades
6. **Code Coverage** - Generar reportes de cobertura
7. **SonarQube** - Análisis de calidad de código
8. **Package** - Crear artefactos (JAR, Docker image)
9. **Deploy** - Desplegar a entorno de staging/producción

---

## 🔌 Configuración para API Gateway

### Paso 1: Crear el archivo de workflow

Crea el archivo `.github/workflows/ci-cd.yml` en el proyecto **API Gateway**:

```yaml
name: API Gateway CI/CD Pipeline

on:
  push:
    branches: [ main, develop, master ]
  pull_request:
    branches: [ main, develop, master ]

env:
  JAVA_VERSION: '21'
  SERVICE_NAME: 'api-gateway'

jobs:
  test:
    name: Tests y Cobertura
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: apigateway
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar tests
      run: mvn clean test
      env:
        DATABASE_URL: jdbc:postgresql://localhost:5432/apigateway
        DATABASE_USERNAME: postgres
        DATABASE_PASSWORD: postgres
        REDIS_HOST: localhost
        REDIS_PORT: 6379
        VAULT_ENABLED: false
    
    - name: Generar reporte de cobertura
      run: mvn jacoco:report
    
    - name: Subir cobertura a Codecov
      uses: codecov/codecov-action@v4
      with:
        files: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: false
        token: ${{ secrets.CODECOV_TOKEN }}

  static-analysis:
    name: Análisis Estático
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar Checkstyle
      run: mvn checkstyle:check
      continue-on-error: true
    
    - name: Ejecutar SpotBugs
      run: mvn spotbugs:check
      continue-on-error: true
    
    - name: Publicar reportes
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: static-analysis-reports
        path: |
          target/checkstyle-result.xml
          target/spotbugsXml.xml
        retention-days: 7

  security-scan:
    name: Análisis de Seguridad
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar OWASP Dependency Check
      run: mvn org.owasp:dependency-check-maven:check
      continue-on-error: true
    
    - name: Ejecutar Snyk Security Scan
      uses: snyk/actions/maven@master
      continue-on-error: true
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --severity-threshold=high --all-projects

  sonarqube:
    name: Análisis SonarQube
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
      with:
        fetch-depth: 0
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar análisis SonarQube
      run: mvn sonar:sonar
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        SONAR_PROJECT_KEY: ${{ env.SERVICE_NAME }}

  build:
    name: Build y Package
    needs: [test, static-analysis]
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Build con Maven
      run: mvn clean package -DskipTests
    
    - name: Construir imagen Docker
      run: docker build -t ${{ env.SERVICE_NAME }}:${{ github.sha }} .
    
    - name: Publicar artefactos
      uses: actions/upload-artifact@v4
      with:
        name: jar-artifact
        path: target/*.jar
        retention-days: 7

  deploy:
    name: Despliegue
    needs: [build, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Descargar artefactos
      uses: actions/download-artifact@v4
      with:
        name: jar-artifact
    
    - name: Desplegar
      run: |
        echo "Configurar despliegue según tu infraestructura"
        # Ejemplo para Docker Hub:
        # docker login -u ${{ secrets.DOCKER_USERNAME }} -p ${{ secrets.DOCKER_PASSWORD }}
        # docker tag ${{ env.SERVICE_NAME }}:${{ github.sha }} ${{ secrets.DOCKER_USERNAME }}/${{ env.SERVICE_NAME }}:latest
        # docker push ${{ secrets.DOCKER_USERNAME }}/${{ env.SERVICE_NAME }}:latest
```

### Paso 2: Actualizar pom.xml del API Gateway

Agrega los siguientes plugins a la sección `<build><plugins>` de tu `pom.xml`:

```xml
<!-- Checkstyle -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <encoding>UTF-8</encoding>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.12.5</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- SpotBugs -->
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.3.6</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <xmlOutput>true</xmlOutput>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- OWASP Dependency Check -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- SonarQube -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.10.0.2594</version>
</plugin>

<!-- JaCoCo para cobertura -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Paso 3: Copiar checkstyle.xml

Copia el archivo `checkstyle.xml` desde el proyecto principal (uco-challenge) al directorio raíz del API Gateway.

---

## 📦 Configuración para Catalog Service

### Paso 1: Crear el archivo de workflow

Crea el archivo `.github/workflows/ci-cd.yml` en el proyecto **Catalog Service**:

```yaml
name: Catalog Service CI/CD Pipeline

on:
  push:
    branches: [ main, develop, master ]
  pull_request:
    branches: [ main, develop, master ]

env:
  JAVA_VERSION: '21'
  SERVICE_NAME: 'catalog-service'

jobs:
  test:
    name: Tests y Cobertura
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: catalogservice
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7-alpine
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar tests
      run: mvn clean test
      env:
        DATABASE_URL: jdbc:postgresql://localhost:5432/catalogservice
        DATABASE_USERNAME: postgres
        DATABASE_PASSWORD: postgres
        REDIS_HOST: localhost
        REDIS_PORT: 6379
        VAULT_ENABLED: false
    
    - name: Generar reporte de cobertura
      run: mvn jacoco:report
    
    - name: Subir cobertura a Codecov
      uses: codecov/codecov-action@v4
      with:
        files: ./target/site/jacoco/jacoco.xml
        fail_ci_if_error: false
        token: ${{ secrets.CODECOV_TOKEN }}

  static-analysis:
    name: Análisis Estático
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar Checkstyle
      run: mvn checkstyle:check
      continue-on-error: true
    
    - name: Ejecutar SpotBugs
      run: mvn spotbugs:check
      continue-on-error: true
    
    - name: Publicar reportes
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: static-analysis-reports
        path: |
          target/checkstyle-result.xml
          target/spotbugsXml.xml
        retention-days: 7

  security-scan:
    name: Análisis de Seguridad
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar OWASP Dependency Check
      run: mvn org.owasp:dependency-check-maven:check
      continue-on-error: true
    
    - name: Ejecutar Snyk Security Scan
      uses: snyk/actions/maven@master
      continue-on-error: true
      env:
        SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
      with:
        args: --severity-threshold=high --all-projects

  sonarqube:
    name: Análisis SonarQube
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
      with:
        fetch-depth: 0
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Ejecutar análisis SonarQube
      run: mvn sonar:sonar
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        SONAR_PROJECT_KEY: ${{ env.SERVICE_NAME }}

  build:
    name: Build y Package
    needs: [test, static-analysis]
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Set up JDK ${{ env.JAVA_VERSION }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ env.JAVA_VERSION }}
        distribution: 'temurin'
        cache: maven
    
    - name: Build con Maven
      run: mvn clean package -DskipTests
    
    - name: Construir imagen Docker
      run: docker build -t ${{ env.SERVICE_NAME }}:${{ github.sha }} .
    
    - name: Publicar artefactos
      uses: actions/upload-artifact@v4
      with:
        name: jar-artifact
        path: target/*.jar
        retention-days: 7

  deploy:
    name: Despliegue
    needs: [build, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/master'
    
    steps:
    - name: Checkout código
      uses: actions/checkout@v4
    
    - name: Descargar artefactos
      uses: actions/download-artifact@v4
      with:
        name: jar-artifact
    
    - name: Desplegar
      run: |
        echo "Configurar despliegue según tu infraestructura"
```

### Paso 2: Actualizar pom.xml del Catalog Service

Agrega los mismos plugins que se mencionaron para el API Gateway (ver sección anterior).

### Paso 3: Copiar checkstyle.xml

Copia el archivo `checkstyle.xml` desde el proyecto principal al directorio raíz del Catalog Service.

---

## 🔍 Herramientas de Análisis Estático

### Checkstyle

**Propósito**: Verificar que el código siga estándares de estilo y convenciones.

**Configuración**:
- Archivo: `checkstyle.xml` en la raíz del proyecto
- Se ejecuta en la fase `validate` de Maven
- Genera reportes en `target/checkstyle-result.xml`

**Comandos manuales**:
```bash
# Verificar código
mvn checkstyle:check

# Generar reporte HTML
mvn checkstyle:checkstyle
```

### SpotBugs

**Propósito**: Detectar bugs potenciales en el código Java.

**Configuración**:
- Se ejecuta automáticamente en el pipeline
- Genera reportes XML en `target/spotbugsXml.xml`

**Comandos manuales**:
```bash
# Ejecutar análisis
mvn spotbugs:check

# Generar reporte HTML
mvn spotbugs:gui
```

### SonarQube

**Propósito**: Análisis completo de calidad de código, duplicación, cobertura, y más.

**Configuración requerida**:

1. **Crear proyecto en SonarQube**:
   - Accede a tu instancia de SonarQube (cloud o servidor propio)
   - Crea un nuevo proyecto
   - Genera un token de acceso

2. **Configurar secrets en GitHub**:
   - Ve a Settings > Secrets and variables > Actions
   - Agrega los siguientes secrets:
     - `SONAR_TOKEN`: Token generado en SonarQube
     - `SONAR_HOST_URL`: URL de tu instancia (ej: https://sonarcloud.io)

3. **Archivo sonar-project.properties** (opcional, en la raíz del proyecto):
```properties
sonar.projectKey=catalog-service
sonar.projectName=Catalog Service
sonar.projectVersion=1.0
sonar.sources=src/main
sonar.tests=src/test
sonar.java.binaries=target/classes
sonar.java.test.binaries=target/test-classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

---

## 🔒 Configuración de Seguridad

### OWASP Dependency Check

**Propósito**: Escanear dependencias en busca de vulnerabilidades conocidas.

**Configuración**:
- Se ejecuta automáticamente en el pipeline
- Genera reportes HTML en `target/dependency-check-report.html`

**Comandos manuales**:
```bash
# Ejecutar análisis
mvn org.owasp:dependency-check-maven:check

# Ver reporte
open target/dependency-check-report.html
```

### Snyk

**Propósito**: Análisis avanzado de vulnerabilidades y licencias.

**Configuración requerida**:

1. **Crear cuenta en Snyk**:
   - Visita https://snyk.io
   - Crea una cuenta gratuita
   - Genera un token de API

2. **Configurar secret en GitHub**:
   - Agrega `SNYK_TOKEN` en los secrets del repositorio

3. **El workflow ya está configurado** para usar Snyk automáticamente.

---

## 🧪 Pruebas Automatizadas

### Configuración de Tests

Asegúrate de que tus tests sigan estas convenciones:

1. **Nomenclatura**:
   - Tests unitarios: `*Test.java`
   - Tests de integración: `*IT.java` o `*IntegrationTest.java`

2. **Estructura de tests**:
```java
@DisplayName("Tests para [NombreClase]")
class NombreClaseTest {
    
    @Test
    @DisplayName("Debería [comportamiento esperado]")
    void should[Comportamiento]() {
        // Given
        // When
        // Then
    }
}
```

3. **Cobertura mínima**: 80% de líneas de código

### Ejecutar tests localmente

```bash
# Todos los tests
mvn test

# Solo tests unitarios
mvn test -Dtest=*Test

# Solo tests de integración
mvn verify -Dtest=*IT
```

---

## 🚢 Despliegue Continuo

### Opciones de Despliegue

#### Opción 1: Docker Hub

```yaml
- name: Login a Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKER_USERNAME }}
    password: ${{ secrets.DOCKER_PASSWORD }}

- name: Push imagen
  run: |
    docker tag ${{ env.SERVICE_NAME }}:${{ github.sha }} ${{ secrets.DOCKER_USERNAME }}/${{ env.SERVICE_NAME }}:latest
    docker push ${{ secrets.DOCKER_USERNAME }}/${{ env.SERVICE_NAME }}:latest
```

#### Opción 2: AWS ECR

```yaml
- name: Configure AWS credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
    aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
    aws-region: us-east-1

- name: Login a Amazon ECR
  id: login-ecr
  uses: aws-actions/amazon-ecr-login@v2

- name: Build y push imagen
  env:
    ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
    ECR_REPOSITORY: ${{ env.SERVICE_NAME }}
    IMAGE_TAG: ${{ github.sha }}
  run: |
    docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
    docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
```

#### Opción 3: Kubernetes

```yaml
- name: Configurar kubectl
  uses: azure/setup-kubectl@v3

- name: Desplegar a Kubernetes
  run: |
    kubectl set image deployment/${{ env.SERVICE_NAME }} ${{ env.SERVICE_NAME }}=${{ secrets.DOCKER_REGISTRY }}/${{ env.SERVICE_NAME }}:${{ github.sha }}
```

---

## 🛠️ Troubleshooting

### Problemas Comunes

#### 1. Error: "Checkstyle configuration file not found"

**Solución**: Asegúrate de que `checkstyle.xml` esté en la raíz del proyecto y que la ruta en `pom.xml` sea correcta.

#### 2. Error: "SonarQube analysis failed"

**Solución**: 
- Verifica que los secrets `SONAR_TOKEN` y `SONAR_HOST_URL` estén configurados
- Asegúrate de que el proyecto exista en SonarQube
- Verifica que el token tenga permisos suficientes

#### 3. Error: "Tests failing in CI but passing locally"

**Solución**:
- Verifica que las variables de entorno estén configuradas correctamente
- Asegúrate de que los servicios (PostgreSQL, Redis) estén disponibles
- Revisa los logs del workflow para más detalles

#### 4. Error: "Docker build failed"

**Solución**:
- Verifica que el `Dockerfile` esté presente
- Asegúrate de que todas las dependencias estén disponibles
- Revisa que el contexto de build sea correcto

### Verificación Local

Antes de hacer push, ejecuta localmente:

```bash
# 1. Compilar
mvn clean compile

# 2. Ejecutar tests
mvn test

# 3. Análisis estático
mvn checkstyle:check
mvn spotbugs:check

# 4. Análisis de seguridad
mvn org.owasp:dependency-check-maven:check

# 5. Build completo
mvn clean package

# 6. Construir Docker
docker build -t test-image .
```

---

## 📝 Checklist de Implementación

### Para API Gateway

- [ ] Crear `.github/workflows/ci-cd.yml`
- [ ] Agregar plugins al `pom.xml`
- [ ] Copiar `checkstyle.xml` a la raíz
- [ ] Configurar secrets en GitHub:
  - [ ] `SONAR_TOKEN`
  - [ ] `SONAR_HOST_URL`
  - [ ] `SNYK_TOKEN` (opcional)
  - [ ] `CODECOV_TOKEN` (opcional)
- [ ] Verificar que los tests pasen localmente
- [ ] Hacer push y verificar que el pipeline se ejecute

### Para Catalog Service

- [ ] Crear `.github/workflows/ci-cd.yml`
- [ ] Agregar plugins al `pom.xml`
- [ ] Copiar `checkstyle.xml` a la raíz
- [ ] Configurar secrets en GitHub (mismos que API Gateway)
- [ ] Verificar que los tests pasen localmente
- [ ] Hacer push y verificar que el pipeline se ejecute

---

## 📚 Recursos Adicionales

- [Documentación de GitHub Actions](https://docs.github.com/en/actions)
- [Documentación de Checkstyle](https://checkstyle.sourceforge.io/)
- [Documentación de SpotBugs](https://spotbugs.github.io/)
- [Documentación de SonarQube](https://docs.sonarqube.org/)
- [Documentación de OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/)
- [Documentación de JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/)

---

## ✅ Validación Final

Una vez implementado, el pipeline debe:

1. ✅ Ejecutarse automáticamente en cada push y PR
2. ✅ Ejecutar todos los tests
3. ✅ Generar reportes de cobertura (mínimo 80%)
4. ✅ Ejecutar análisis estático (Checkstyle, SpotBugs)
5. ✅ Ejecutar análisis de seguridad (OWASP, Snyk)
6. ✅ Enviar resultados a SonarQube
7. ✅ Construir artefactos (JAR, Docker image)
8. ✅ Desplegar automáticamente en main/master (si está configurado)

---

**Nota**: Esta guía asume que ambos proyectos (API Gateway y Catalog Service) son proyectos Spring Boot con Maven. Si usan Gradle u otra herramienta, adapta los comandos y configuraciones según corresponda.

