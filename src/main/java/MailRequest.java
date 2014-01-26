/**
 * Created by alexpeptan on 24/01/14.
 */
public class MailRequest extends Request {
    public MailRequest(long clientID) {
        setClientID(clientID);
    }

    @Override
    public RequestType getType() {
        return RequestType.MAIL;
    }
}
