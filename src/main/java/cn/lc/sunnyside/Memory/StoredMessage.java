package cn.lc.sunnyside.Memory;

/**
 * Redis 中持久化的消息结构。
 */

public class StoredMessage {

    private String type;
    private String text;

    public StoredMessage() {
    }

    public StoredMessage(String type, String text) {
        this.type = type;
        this.text = text;
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

