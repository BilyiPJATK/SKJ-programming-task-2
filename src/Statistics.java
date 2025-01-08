import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public class Statistics {
    private final AtomicInteger clientCount = new AtomicInteger();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final AtomicInteger invalidCount = new AtomicInteger();
    private final AtomicInteger resultSum = new AtomicInteger();
    private final ConcurrentHashMap<String, AtomicInteger> operationCounts = new ConcurrentHashMap<>();

    public Statistics() {
        operationCounts.put("ADD", new AtomicInteger());
        operationCounts.put("SUB", new AtomicInteger());
        operationCounts.put("MUL", new AtomicInteger());
        operationCounts.put("DIV", new AtomicInteger());
    }

    public void incrementClientCount() {
        clientCount.incrementAndGet();
    }

    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    public void incrementInvalidCount() {
        invalidCount.incrementAndGet();
    }

    public void incrementOperationCount(String operation) {
        operationCounts.get(operation).incrementAndGet();
    }

    public void addResultSum(int value) {
        resultSum.addAndGet(value);
    }

    public void printStatistics() {
        System.out.println("=== Global Statistics ===");
        System.out.println("Clients connected: " + clientCount.get());
        System.out.println("Total requests: " + requestCount.get());
        System.out.println("Invalid requests: " + invalidCount.get());
        System.out.println("Results sum: " + resultSum.get());
        operationCounts.forEach((operation, count) ->
                System.out.println(operation + " operations: " + count.get())
        );
        System.out.println("=========================");
    }
}
