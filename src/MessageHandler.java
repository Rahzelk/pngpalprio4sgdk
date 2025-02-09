
public class MessageHandler 
{
    private final int code;
    private final String message;
    private final int type;

    public MessageHandler(int code, String message, int type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
    

    public int getType() {
        return type;
    }
}