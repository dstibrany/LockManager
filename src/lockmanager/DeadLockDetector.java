package lockmanager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DeadLockDetector {
    private WaitForGraph waitForGraph;

    DeadLockDetector(WaitForGraph waitForGraph) {
        this.waitForGraph = waitForGraph;
    }

    void test() {
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(() -> {
            System.out.println("Hello world");
        });
    }
}
