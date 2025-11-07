package co.edu.uco.parameterservices.catalog.message.service;

import java.util.Map;
import org.springframework.stereotype.Service;

import co.edu.uco.parameterservices.catalog.message.MessageCatalog;
import co.edu.uco.parameterservices.catalog.message.domain.Message;

@Service
public class MessageService {

    private final MessageCatalog catalog;

    public MessageService(MessageCatalog catalog) {
        this.catalog = catalog;
    }

    public Message findByCode(String code) {
        return catalog.getMessage(code);
    }

    public Map<String, Message> findAll() {
        return catalog.getAllMessages();
    }
}
