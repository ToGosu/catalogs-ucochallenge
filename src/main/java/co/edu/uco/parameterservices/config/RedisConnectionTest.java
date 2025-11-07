package co.edu.uco.parameterservices.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisConnectionTest implements CommandLineRunner {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Override
    public void run(String... args) {
        try {
            var connection = connectionFactory.getConnection();
            System.out.println("Redis conectado correctamente: " + connection.ping());
        } catch (Exception e) {
            System.out.println("No se pudo conectar a Redis: " + e.getMessage());
        }
    }
}
