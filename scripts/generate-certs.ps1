# Script PowerShell para generar certificados SSL autofirmados
# Uso: .\scripts\generate-certs.ps1

$CERT_DIR = "certs"
$DAYS_VALID = 365

# Crear directorio de certificados si no existe
if (-not (Test-Path $CERT_DIR)) {
    New-Item -ItemType Directory -Path $CERT_DIR | Out-Null
}

# Verificar si OpenSSL está disponible
$opensslPath = Get-Command openssl -ErrorAction SilentlyContinue

if (-not $opensslPath) {
    Write-Host "OpenSSL no está instalado. Instalando certificado usando PowerShell..." -ForegroundColor Yellow
    
    # Generar certificado usando PowerShell (requiere Windows 10+)
    $cert = New-SelfSignedCertificate `
        -DnsName "localhost" `
        -CertStoreLocation "cert:\LocalMachine\My" `
        -KeyAlgorithm RSA `
        -KeyLength 2048 `
        -NotAfter (Get-Date).AddDays($DAYS_VALID)
    
    # Exportar certificado
    $certPath = "cert:\LocalMachine\My\$($cert.Thumbprint)"
    Export-Certificate -Cert $certPath -FilePath "$CERT_DIR\server.crt" | Out-Null
    
    # Exportar clave privada (requiere conversión adicional)
    $pwd = ConvertTo-SecureString -String "temp" -Force -AsPlainText
    Export-PfxCertificate -Cert $certPath -FilePath "$CERT_DIR\server.pfx" -Password $pwd | Out-Null
    
    Write-Host "Certificado generado usando PowerShell" -ForegroundColor Green
    Write-Host "NOTA: Para usar con Nginx, necesitas convertir el .pfx a .key usando OpenSSL" -ForegroundColor Yellow
    Write-Host "Si tienes OpenSSL instalado, ejecuta: openssl pkcs12 -in certs/server.pfx -nocerts -nodes -out certs/server.key" -ForegroundColor Yellow
} else {
    # Usar OpenSSL si está disponible
    Write-Host "Generando certificados usando OpenSSL..." -ForegroundColor Green
    
    # Generar clave privada
    & openssl genrsa -out "$CERT_DIR\server.key" 2048
    
    # Generar certificado autofirmado
    & openssl req -new -x509 -key "$CERT_DIR\server.key" -out "$CERT_DIR\server.crt" -days $DAYS_VALID `
        -subj "/C=CO/ST=Cundinamarca/L=Bogota/O=UCO/OU=IS2/CN=localhost"
    
    Write-Host "Certificados SSL generados en $CERT_DIR\" -ForegroundColor Green
    Write-Host "Certificado: $CERT_DIR\server.crt" -ForegroundColor Cyan
    Write-Host "Clave privada: $CERT_DIR\server.key" -ForegroundColor Cyan
}

