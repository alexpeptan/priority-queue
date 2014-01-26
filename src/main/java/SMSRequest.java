/**
 * Created by alexpeptan on 24/01/14.
 */
public class SMSRequest extends Request{
    public SMSRequest(long clientID) {
        setClientID(clientID);
    }

    @Override
    public RequestType getType() {
        return RequestType.SMS;
    }
}
