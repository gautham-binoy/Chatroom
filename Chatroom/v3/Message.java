import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType {
        TEXT, CODE, FILE, SYSTEM, USER_LIST
    }

    private MessageType type;
    private String from;
    private String to;  // null for broadcast
    private String content;
    private byte[] fileData;
    private String fileName;

    public Message(MessageType type, String from, String to, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
    }

    public Message(String from, String to, String fileName, byte[] fileData) {
        this.type = MessageType.FILE;
        this.from = from;
        this.to = to;
        this.fileName = fileName;
        this.fileData = fileData;
    }

    // Getters
    public MessageType getType() { return type; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getContent() { return content; }
    public byte[] getFileData() { return fileData; }
    public String getFileName() { return fileName; }
}
