package co.edu.uco.parameterservices.catalog.message.domain;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "message")
public class Message implements Serializable {

    @Id
    private String code;
    private String type; // e.g., ERROR, SUCCESS, INFO
    private String text;

    public Message() {
        // Constructor vacío requerido por JPA y para serialización
    }

    public Message(String code, String type, String text) {
        this.code = code;
        this.type = type;
        this.text = text;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
