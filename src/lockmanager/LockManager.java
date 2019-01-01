package lockmanager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

class LockManager {
    private ConcurrentHashMap<Integer, Lock> lockTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Transaction, ArrayList<Lock>> txnTable = new ConcurrentHashMap<>();
    private WaitForGraph waitForGraph = new WaitForGraph();

    LockManager() {
        // TODO: figure out how to pass in DL params
        waitForGraph.startDetectionLoop(1000, 5000);
    }

   void lock(Integer lockName, Transaction txn, Lock.LockMode requestedMode) throws DeadlockException {
       lockTable.putIfAbsent(lockName, new Lock(waitForGraph));
       Lock lock = lockTable.get(lockName);

       try {
           if (requestedMode == Lock.LockMode.SHARED && hasLock(txn, lockName) && lock.getMode() == Lock.LockMode.SHARED) {
               return;
           }
           else if (requestedMode == Lock.LockMode.EXCLUSIVE && hasLock(txn, lockName) && lock.getMode() == Lock.LockMode.EXCLUSIVE) {
               return;
           }
           else if (requestedMode == Lock.LockMode.EXCLUSIVE && hasLock(txn, lockName) && lock.getMode() == Lock.LockMode.SHARED) {
               lock.upgrade(txn);
           }
           else {
               lock.acquire(txn, requestedMode);
           }
       } catch (InterruptedException e) {
           removeTransaction(txn);
           throw new DeadlockException(e);
       }

       txnTable.putIfAbsent(txn, new ArrayList<>());
       ArrayList<Lock> txnLockList = txnTable.get(txn);
       txnLockList.add(lock);
       txnTable.put(txn, txnLockList);
   }

   void removeTransaction(Transaction txn) {
        ArrayList<Lock> txnLockList = txnTable.get(txn);

        for (Lock lock: txnLockList) {
            lock.release(txn);
        }

        txnTable.remove(txn);
   }

   boolean hasLock(Transaction txn, Integer lockName) {
       ArrayList<Lock> lockList = txnTable.get(txn);
       Lock lock = lockTable.get(lockName);

       if (lockList == null) return false;
       boolean found = false;

       for (Lock txnLock: txnTable.get(txn)) {
           if (txnLock == lock) found = true;
       }

       return found;
   }

   Lock.LockMode getLockMode(Integer lockName) {
       return lockTable.get(lockName).getMode();
   }

   WaitForGraph getWaitForGraph() {
       return waitForGraph;
   }
}

