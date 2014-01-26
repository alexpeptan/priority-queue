import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by alexpeptan on 24/01/14.
 */
public class RequestsSelfAdjustingQueueTest {
    @Test
    public void addFirstElement() throws InvalidPolicyException {
        // when one mail request is created and added to RequestsSelfAdjustingQueue
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();
        int clientID = 1;
        MailRequest mailRequest = new MailRequest(clientID);
        // then it is added to the queue
        queue.add(mailRequest);
        // assert queue size increased to 1
        Assert.assertEquals(1, queue.getQueueSize());
        Request pulledRequest = queue.get();
        Assert.assertEquals(0, queue.getQueueSize());
        Assert.assertEquals(pulledRequest, mailRequest);
    }

    @Test
    public void addElementsFromOneClient() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();
        int mailNo = 5;
        int clientID = 1;
        addNElementsToQueue(queue, mailNo, clientID, RequestType.MAIL);
        Assert.assertEquals(mailNo, queue.getQueueSize());
        for(int i = 0; i < mailNo; i++){
            queue.get();
        }
        Assert.assertEquals(0, queue.getQueueSize());
    }

    @Test
    public void removeElementFromEmptyQueue() throws Exception {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        queue.get();
    }

//    enum RequestType { MAIL, SMS, RANDOM};

    public void addNElementsToQueue(RequestsSelfAdjustingQueue queue,long noOfElements, int clientID, RequestType type){
        Request request = null;
        for(int i = 0; i < noOfElements; i++){
            switch(type) {
                case MAIL:   request = new MailRequest(clientID);
                             break;
                case SMS:    request = new SMSRequest(clientID);
                             break;
                case RANDOM: Random r = new Random();
                             int x = r.nextInt(2);
                             if(x == 0) {
                                 request = new MailRequest(clientID);
                             } else {
                                 request = new SMSRequest(clientID);
                             }
            }
            queue.add(request);
        }
    }

    public void consumeAllElementsFromQueueInstantly(RequestsSelfAdjustingQueue queue){
        while(queue.getQueueSize() > 0){
            queue.get();
        }
    }

    public void consumeAllElementsFromQueueRealTime(RequestsSelfAdjustingQueue queue){
        Request request = null;
        while(queue.getQueueSize() > 0){
            request = queue.get();
            RequestType type = request.getType();
            long timeToSleep = RequestExecutionStatistics.getAverageExecutionTime(type);
            try {
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void addElementsFrom2ClientsBothDemoted() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 3, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 1, 2, RequestType.SMS);
        Assert.assertEquals(4, queue.getQueueSize());
        consumeAllElementsFromQueueInstantly(queue);
        Assert.assertEquals(0, queue.getQueueSize());
    }

    @Test
    public void addElementsFrom2ClientsOnlyOneDemoted() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 4, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 1, 2, RequestType.SMS);
        Assert.assertEquals(5, queue.getQueueSize());
        consumeAllElementsFromQueueInstantly(queue);
        Assert.assertEquals(0, queue.getQueueSize());
    }

    @Test
    public void addElementsClientFairConsumptionOnSameSeverityLevel() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 4, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 4, 2, RequestType.SMS);
        addNElementsToQueue(queue, 4, 3, RequestType.MAIL);
        addNElementsToQueue(queue, 4, 4, RequestType.SMS);
        Assert.assertEquals(16, queue.getQueueSize());
        consumeAllElementsFromQueueInstantly(queue);
        Assert.assertEquals(0, queue.getQueueSize());
    }

    @Test
    public void addElementsClientFairRealTimeConsumptionOnSameSeverityLevel() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 4, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 4, 2, RequestType.SMS);
        addNElementsToQueue(queue, 4, 3, RequestType.MAIL);
        addNElementsToQueue(queue, 4, 4, RequestType.SMS);
        Assert.assertEquals(16, queue.getQueueSize());
        consumeAllElementsFromQueueRealTime(queue);
        Assert.assertEquals(0, queue.getQueueSize());
    }

    @Test
    public void promotionTest() throws InvalidPolicyException {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 8, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 80, 2, RequestType.SMS);
        consumeAllElementsFromQueueRealTime(queue);
    }

    @Test
    public void fairConsumptionForAllClients() throws Exception {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        addNElementsToQueue(queue, 10, 1, RequestType.MAIL);
        addNElementsToQueue(queue, 10, 2, RequestType.SMS);

        //
        List<Request> consumed = consumeNRequestsFromQueue(queue, 10);
        assertContainsRequestsForClient(consumed, 2L);
        assertContainsRequestsForClient(consumed, 1L);
    }

    @Test
    public void allowsMultipleProducersSameClientId() throws Exception {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        // Create N threads that call addNElementsToQueue

        // Thread.sleep(...)
    }

    @Test
    public void allowsMultipleProducersDifferentClientId() throws Exception {
        RequestsSelfAdjustingQueue queue = new RequestsSelfAdjustingQueue();

        // Create N threads that call addNElementsToQueue

        // Thread.sleep(...)
    }

    private static void assertContainsRequestsForClient(List<Request> requests, Long clientId) {
        for (Request r: requests) {
            if (r.getClientID() == clientId) {
                return;
            }
        }
        throw new AssertionError("No requests for client with id " + clientId);
    }

    private List<Request> consumeNRequestsFromQueue(RequestsSelfAdjustingQueue queue, int nRequests) {
        List<Request> requestsConsumed = new ArrayList<Request>();
        for (int i = 0; i < nRequests; i++) {
            requestsConsumed.add(queue.get());
        }
        return requestsConsumed;
    }
}
