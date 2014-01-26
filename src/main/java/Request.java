/**
 * Created by alexpeptan on 24/01/14.
 */
enum RequestType { MAIL, SMS, RANDOM};

public abstract class Request {
    private long clientID;
    private long requestID;
    private long timeAddedToQueue; // in milliseconds
    private long timeRemovedFromQueue; // in milliseconds
    private static long requestIDSequence = 0;

    public Request() {
        setRequestID(getNextRequestID());
    }

    public static long getNextRequestID(){
        return ++requestIDSequence;
    }

    public long getClientID() {
        return clientID;
    }

    public void setClientID(long clientID) {
        this.clientID = clientID;
    }

    public long getRequestID() {
        return requestID;
    }

    public void setRequestID(long requestID) {
        this.requestID = requestID;
    }

    public long getTimeAddedToQueue() {
        return timeAddedToQueue;
    }

    public void setTimeAddedToQueue(long timeAddedToQueue) {
        this.timeAddedToQueue = timeAddedToQueue;
    }

    public long getTimeRemovedFromQueue() {
        return timeRemovedFromQueue;
    }

    public void setTimeRemovedFromQueue(long timeRemovedFromQueue) {
        this.timeRemovedFromQueue = timeRemovedFromQueue;
    }

    public abstract RequestType getType();
}
