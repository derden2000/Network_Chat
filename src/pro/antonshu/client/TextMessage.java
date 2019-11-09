package pro.antonshu.client;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TextMessage implements Serializable {

    private LocalDateTime created;

    private String userFrom;

    private String userTo;

    private String text;

    public TextMessage(String userFrom, String userTo, String text) {
        this.created = LocalDateTime.now();
        this.userFrom = userFrom;
        this.userTo = userTo;
        this.text = text;
    }

    public String getUserFrom() {
        return userFrom;
    }

    public String getUserTo() {
        return userTo;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreated() {
        return created;
    }
}
