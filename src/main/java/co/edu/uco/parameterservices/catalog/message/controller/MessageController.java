package co.edu.uco.parameterservices.catalog.message.controller;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import co.edu.uco.parameterservices.catalog.message.domain.Message;
import co.edu.uco.parameterservices.catalog.message.service.MessageService;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Message>> getAllMessages() {
        return new ResponseEntity<>(service.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Message> findByCode(@PathVariable String code) {
        Message message = service.findByCode(code);
        return new ResponseEntity<>(message, message == null ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }
}
