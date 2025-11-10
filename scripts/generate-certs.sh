#!/bin/bash

# Script para generar certificados SSL autofirmados
# Uso: ./scripts/generate-certs.sh

CERT_DIR="certs"
DAYS_VALID=365

# Crear directorio de certificados si no existe
mkdir -p $CERT_DIR

# Generar clave privada
openssl genrsa -out $CERT_DIR/server.key 2048

# Generar certificado autofirmado
openssl req -new -x509 -key $CERT_DIR/server.key -out $CERT_DIR/server.crt -days $DAYS_VALID \
    -subj "/C=CO/ST=Cundinamarca/L=Bogota/O=UCO/OU=IS2/CN=localhost"

# Establecer permisos
chmod 600 $CERT_DIR/server.key
chmod 644 $CERT_DIR/server.crt

echo "Certificados SSL generados en $CERT_DIR/"
echo "Certificado: $CERT_DIR/server.crt"
echo "Clave privada: $CERT_DIR/server.key"

