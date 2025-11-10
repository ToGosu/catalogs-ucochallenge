package co.edu.uco.parameterservices.sender;

import org.springframework.stereotype.Service;

/**
 * Servicio para enviar emails/SMS reales
 * Este es un ejemplo básico - en producción usar SendGrid/Twilio
 */
@Service
public class EmailSenderService {

    /**
     * Envía un email
     * @param to Destinatario
     * @param subject Asunto
     * @param body Cuerpo (puede ser HTML)
     * @param format HTML o PLAIN_TEXT
     */
    public void sendEmail(String to, String subject, String body, String format) {
        // TODO: Implementar con JavaMailSender o API externa
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("📧 EMAIL ENVIADO");
        System.out.println("═══════════════════════════════════════");
        System.out.println("Para: " + to);
        System.out.println("Asunto: " + subject);
        System.out.println("Formato: " + format);
        System.out.println("Cuerpo:");
        System.out.println(body);
        System.out.println("═══════════════════════════════════════");
    }

    /**
     * Envía un SMS
     * @param to Número de teléfono
     * @param message Mensaje de texto
     */
    public void sendSms(String to, String message) {
        // TODO: Implementar con notification API
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("📱 SMS ENVIADO");
        System.out.println("═══════════════════════════════════════");
        System.out.println("Para: " + to);
        System.out.println("Mensaje: " + message);
        System.out.println("═══════════════════════════════════════");
    }
}