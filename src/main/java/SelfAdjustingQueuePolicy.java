/**
 * Created by alexpeptan on 26/01/14.
 */
public class SelfAdjustingQueuePolicy {
    // initializing with default values
    private int levelsOfSeverity;
    private int[] severityBorder;
    private int[] resourceAllocation;
    private int threshold; // +/- 5%
    private long baseDuration; // total time(milliseconds) allocated for overall execution of requests
                                                     // on all severity levels per iteration through all severity levels.
    private long promotionCheckPeriod; // period(in milliseconds) on which there is a check for needed promotions

    public SelfAdjustingQueuePolicy() throws InvalidPolicyException {
        // validate the default values stated above.
        this(2, new int[] {15}, new int[] {70, 30}, 5, 10000, 2000);
    }

    public SelfAdjustingQueuePolicy(int levelsOfSeverity,
                                    int[] severityBorder,
                                    int[] resourceAllocation,
                                    int threshold,
                                    long baseDuration,
                                    long promotionCheckPeriod) throws InvalidPolicyException {
        this.levelsOfSeverity = levelsOfSeverity;
        this.severityBorder = severityBorder;
        this.resourceAllocation = resourceAllocation;
        this.threshold = threshold;
        this.baseDuration = baseDuration;
        this.promotionCheckPeriod = promotionCheckPeriod;

        validatePolicy();
    }

    private void validatePolicy() throws InvalidPolicyException {
        // validate levelsOfSeverity in [1,10] interval
        if(getLevelsOfSeverity() <= 0 || getLevelsOfSeverity() > 10) {
            throw new InvalidPolicyException("Level of severity outside interval [1,10].");
        }
        // validate that there are levelsOfSeverity-1  valid percent elements in array severityBorder
        for(int i = 0; i < getLevelsOfSeverity() - 1; i++){
            if(severityBorder[i] - threshold <= 0 || severityBorder[i] + threshold >= 100) {
                throw new InvalidPolicyException("severityBorder[i]+-threshold must be in [1,99] interval.");
            }
        }
        // validate resource allocation for each level is in [0,100] interval and the sum of all resource allocation percents is 100%.
        int percentsSum = 0;
        for(int i = 0; i < getLevelsOfSeverity(); i++){
            if(resourceAllocation[i] < 0 || resourceAllocation[i] > 100) {
                throw new InvalidPolicyException("Resource allocation percent for each level must be in [0,100] interval.");
            }
            percentsSum += resourceAllocation[i];
        }
        if(100 != percentsSum){
            throw new InvalidPolicyException("The sum of all resource allocation percents must be 100%.");
        }
        // validate threshold value. Must be in [1,49] interval.
        if(threshold < 1 || threshold > 49){
            throw new InvalidPolicyException("Threshold value must be in [1,49] interval.");
        }
        // validate baseDuration. Must be >= 1000 milliseconds (1 second)
        if(getBaseDuration() < 1000) {
            throw new InvalidPolicyException("baseDuration must be >= 1000 milliseconds (1 second).");
        }
        // valdidate promotionCheckPeriod. Must be >= 100 milliseconds
        if(getPromotionCheckPeriod() < 100) {
            throw new InvalidPolicyException("promotionCheckPeriod must be >= 100 milliseconds.");
        }
    }

    public int[] getSeverityBorder() {
        return severityBorder;
    }

    public int[] getResourceAllocation() {
        return resourceAllocation;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getLevelsOfSeverity() {
        return levelsOfSeverity;
    }

    public long getBaseDuration() {
        return baseDuration;
    }

    public long getPromotionCheckPeriod() {
        return promotionCheckPeriod;
    }
}
