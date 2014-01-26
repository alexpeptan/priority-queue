/**
 * Created by alexpeptan on 24/01/14.
 */
public class RequestExecutionStatistics {
    public static long getAverageExecutionTime(RequestType type){
        switch(type) {
            case MAIL: return 300l;
            case SMS: return 100l;
        }
        throw new RuntimeException("Rearched what wanted to be unreachable code!");
    }
}
