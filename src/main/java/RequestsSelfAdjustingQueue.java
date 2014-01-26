import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by alexpeptan on 24/01/14.
 */
public class RequestsSelfAdjustingQueue {
    /*
     * Considering
     * private int[] severityBorder =  {15,35};
     * private int threshold = 5;
     *
     * A client will be demoted from severity 0 to severity 1 when it has reached more than 15+5 = 20% of the total requests
     * A client will be demoted from severity 1 to severity 2 when it has reached more than 35+5 = 40% of the total requests
     * A client will be promoted from severity 1 to severity 0 when it has reached less than 15-5 = 10% of the total requests
     * A client will be promoted from severity 2 to severity 1 when it has reached less than 35-5 = 30% of the total requests
     *
     * For levelX from 0 to LEVELS_OF_SEVERITY - 2,
     * A client will be demoted from severity levelX to severity levelX + 1 when it has reached
     * more than severityBorder[levelX] + threshold of the total requests.
     * A client will be promoted from severity levelX + 1 to severity levelX when it has reached
     * less than severityBorder[levelX] - threshold of the total requests.
     */
    private int LEVELS_OF_SEVERITY;
    private Hashtable<Long, LinkedBlockingDeque<Request>> severity[];
    private int[] severityBorder;// =  {15};//{15,35};
    private int[] resourceAllocation;// = {70, 30}; // {70, 30, 20}; percent of the available resources - time - allocated per level
    private int threshold;// = 5; // +/- 5%
    private Hashtable<Long, Integer> clientsSeverityLevel = new Hashtable<Long, Integer>();
    private Hashtable<Long, Long> requestsNoForClient = new Hashtable<Long, Long>();
    private AtomicLong totalRequestsNo = new AtomicLong(0L);
    private long BASE_DURATION;// = 10000; // total time(milliseconds) allocated for overall execution of requests
                                                     // on all severity levels per iteration through all severity levels.
    private long PROMOTION_CHECK_PERIOD;// = 2000; // period(in milliseconds) on which there is a check for needed promotions
    private long queueSize = 0;
    SelfAdjustingQueuePolicy policy;

    public void setNewPolicy(SelfAdjustingQueuePolicy newPolicy){
        policy = newPolicy;
        applyPolicy(newPolicy);
    }

    public RequestsSelfAdjustingQueue() throws InvalidPolicyException {
        policy = new SelfAdjustingQueuePolicy();
        initializeQueue();
    }

    public RequestsSelfAdjustingQueue(SelfAdjustingQueuePolicy policy){
        this.policy = policy;

        initializeQueue();
    }

    @SuppressWarnings("unchecked")
    private void initializeQueue() {
        applyPolicy(policy);
        severity = new Hashtable[LEVELS_OF_SEVERITY];
        timeLeftForLevel = new long[LEVELS_OF_SEVERITY];
        levelJustChanged = new boolean[LEVELS_OF_SEVERITY];

        for(int i = 0; i < LEVELS_OF_SEVERITY; i++) {
            initTimeLeftForLevel(i);
            severity[i] = new Hashtable<Long, LinkedBlockingDeque<Request>>();
        }

        // start thread to check for promotions.
        Runnable runnable = new Runnable() {
            public void run(){
                // does this thread ends after test execution ends? When does it end? Do they stuck in the memory?
                while(true){
                    try { // sleep before checking for promotions
                        System.out.println("Severity adjusting thread sleeps " + PROMOTION_CHECK_PERIOD / 1000 + " seconds.");
                        Thread.sleep(PROMOTION_CHECK_PERIOD);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Severity adjusting thread wakes up.");

                    Set<Long> clientIDs = clientsSeverityLevel.keySet();
                    for(Long clientID : clientIDs) {
                        Integer currentClientSeverityLevel = clientsSeverityLevel.get(clientID);
                        if(currentClientSeverityLevel > 0) {
                            // a promotion is possible
                            if(promotionNeeded(clientID, currentClientSeverityLevel)) {
                                promoteClient(clientID, currentClientSeverityLevel);
                            }
                        }
                    }
                }
            }
        };
        Thread t = new Thread(runnable);
        t.start();
    }

    private void applyPolicy(SelfAdjustingQueuePolicy policy) {
        LEVELS_OF_SEVERITY = policy.getLevelsOfSeverity();
        severityBorder =  policy.getSeverityBorder();
        resourceAllocation = policy.getResourceAllocation();
        threshold = policy.getThreshold();
        BASE_DURATION = policy.getBaseDuration();
        PROMOTION_CHECK_PERIOD = policy.getPromotionCheckPeriod();
    }

    private void initTimeLeftForLevel(int l) {
        levelJustChanged[l] = true;// will be needed in get() method
        timeLeftForLevel[l] = BASE_DURATION / 100 * resourceAllocation[l];
    }

    public void add(Request request){
        System.out.println("DEBUG: Added " + request.getClass().getName() + " request. ClientID: " +
                request.getClientID() + ", RequestID: " + request.getRequestID());
        setQueueSize(getQueueSize() + 1);
        long clientID = request.getClientID();
        int currentClientSeverityLevel; // dummy
        if(!clientsSeverityLevel.containsKey(clientID)) {
            // client sends request for the first time
            currentClientSeverityLevel = 0;
            clientsSeverityLevel.put(clientID, 0); // assign highest priority to client by default
            requestsNoForClient.put(clientID, 1l); // count first request from this client
            severity[currentClientSeverityLevel].put(clientID, new LinkedBlockingDeque<Request>());// create its Deque
        } else {
            // adds request to the LinkedBlockingDeque of this specific client in the Hashtable severity[currentClientSeverityLevel]
            currentClientSeverityLevel = clientsSeverityLevel.get(clientID);
            requestsNoForClient.put(clientID, 1 + requestsNoForClient.get(clientID));// count this request for future statistics
        }
        severity[currentClientSeverityLevel].get(clientID).add(request);// adds at the end of the Deque
        totalRequestsNo.incrementAndGet();

        // Set time of adding to Queue for future statistics
        request.setTimeAddedToQueue(System.currentTimeMillis());

        // check demotion conditions for clientID
        if(currentClientSeverityLevel < LEVELS_OF_SEVERITY - 1){
            // does not have the lowest level of severity -> it can still be demoted
            if(demotionNeeded(clientID, currentClientSeverityLevel)) {
                demoteClient(clientID, currentClientSeverityLevel);
            }
        }
    }

    private boolean promotionNeeded(long clientID, int currentLevel) {
        long clientRequestsNo = requestsNoForClient.get(clientID);
        float percentOfTotalRequests = 100f * clientRequestsNo / totalRequestsNo.get();

        return percentOfTotalRequests < severityBorder[currentLevel - 1] - threshold;
    }

    private void promoteClient(long clientID, int currentClientSeverityLevel) {
        System.out.println("DEBUG: Promote client " + clientID + " to level " + (currentClientSeverityLevel -1));
        clientsSeverityLevel.put(clientID, currentClientSeverityLevel - 1);
        LinkedBlockingDeque clientDeque = severity[currentClientSeverityLevel].get(clientID);
        // remove Deque from Hashtable severity[currentClientSeverityLevel]
        severity[currentClientSeverityLevel].remove(clientID);
        // add Deque to Hashtable severity[currentClientSeverityLevel - 1] -> next lower severity level
        severity[currentClientSeverityLevel - 1].put(clientID, clientDeque);
    }

    private boolean demotionNeeded(long clientID, int currentLevel) {
        long clientRequestsNo = requestsNoForClient.get(clientID);
        float percentOfTotalRequests = 100f * clientRequestsNo / totalRequestsNo.get();

        if(percentOfTotalRequests > severityBorder[currentLevel] + threshold) {
            System.out.println("DEBUG: Demotion of client " + clientID + " needed. PercentOfTotalRequests = " + percentOfTotalRequests +
            " and it is larger than the value " + (severityBorder[currentLevel] + threshold));
        }

        return percentOfTotalRequests > severityBorder[currentLevel] + threshold;
    }

    private void demoteClient(long clientID, int currentClientSeverityLevel) {
        System.out.println("DEBUG: Demote client " + clientID + " to level " + (currentClientSeverityLevel + 1));
        LinkedBlockingDeque clientDeque = severity[currentClientSeverityLevel].get(clientID);
        // remove Deque from Hashtable severity[currentClientSeverityLevel]
        severity[currentClientSeverityLevel].remove(clientID);
        // add Deque to Hashtable severity[currentClientSeverityLevel + 1] -> next lower severity level
        severity[currentClientSeverityLevel + 1].put(clientID, clientDeque);
        clientsSeverityLevel.put(clientID, currentClientSeverityLevel + 1);
    }

    private int level = 0; // current level from which a request will be extracted
    private long timeLeftForLevel[]; // in milliseconds
    private boolean levelJustChanged[];
    Enumeration<LinkedBlockingDeque<Request>> currentLevelDeques;
    /*
     * Policy used for extracting the next Request:
     * 1. On each level in the queue there is a certain quantity of resources - time - destined
     * to be used on requests from that severity level. That value is stored in timeLeftForLevel[level].
     * 2. The time needed for a certain request type is a statistical estimation that is periodically adjusted.
     * 3. If there is no other request on the current level to be executed and there is still time left,
     * the timeLeftForLevel[level] will be set to its initial duration and the level will be changed.
     * Otherwise, the level will be changed after timeLeftForLevel[level] becomes negative, but not before
     * setting it back to its initial duration.
     * 4. Durations are set in direct proportion with resourceAllocation percents.
     * 5. Removes element from the start of the selected client's Deque
     * 6. Cycles through all clients' Deques, getting one element from each Deque for reasons of fairness.
     *
     * throws NoSuchElementException when the whole Queue is empty
     */
    public Request get() throws NoSuchElementException{
        setQueueSize(getQueueSize() - 1);
        // here comes the whole story of the policy used to get the current request.
        LinkedBlockingDeque<Request> currentDeque = null;
        Request request = null;
        if(timeLeftForLevel[level] > 0 ) {
            // Select current Deque
            boolean dequeSelected = false;
            while(!dequeSelected){
                try{
                    if(levelJustChanged[level]) {
                        levelJustChanged[level] = false;
                        currentLevelDeques = severity[level].elements();
                    }
                    currentDeque = currentLevelDeques.nextElement();
                    dequeSelected = true;
                } catch(NoSuchElementException ex) {
                    // start iterating again through all clients' Deques on this level
                    currentLevelDeques = severity[level].elements();
                    if(!currentLevelDeques.hasMoreElements()) {
                        // indeed there are no requests left on this level
                        goToNextSeverityLevel();
                    } else {
                        currentDeque = currentLevelDeques.nextElement();
                        dequeSelected = true;
                    }
                }
            }


            // for sure there is at least one element. References to empty Deques are set to null
            request = currentDeque.removeFirst();
            System.out.println("DEBUG: Removed " + request.getClass().getName() + " request. ClientID: " +
                    request.getClientID() + ", RequestID: " + request.getRequestID());

            timeLeftForLevel[level] -= RequestExecutionStatistics.getAverageExecutionTime(request.getType());

            // Set time of removing from Queue for future statistics
            request.setTimeRemovedFromQueue(System.currentTimeMillis());

            // test if Deque is empty now.
            if(currentDeque.size() == 0) {
                // remove Deque from Hashtable
                severity[level].remove(request.getClientID());
            }
        } else {
            // no more time left to solve requests from this level
            goToNextSeverityLevel();
            request = get();
        }

        return request;
    }

    private void goToNextSeverityLevel() {
        initTimeLeftForLevel(level);
        level = (level + 1) % LEVELS_OF_SEVERITY; // next level

        // are all other levels empty?
        // It is possible to be in a chain of recursive calls. For that situation, throw NoSuchElementException
        boolean queueEmpty = true;
        for(int i = 0; i < LEVELS_OF_SEVERITY; i++){
            if(severity[i].size() != 0) {
                queueEmpty = false;
                break;
            }
        }
        if(queueEmpty) {
            throw new NoSuchElementException("Queue empty");
        }
    }

    /*
     * for cases where the request failed to be executed.
     */
    public void putBackToQueue(Request request){

    }


    public long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(long queueSize) {
        this.queueSize = queueSize;
    }
}
