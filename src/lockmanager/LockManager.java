package lockmanager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class LockManager {
    private ConcurrentHashMap<Integer, Lock> lockTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Transaction, ArrayList<Lock>> txnTable = new ConcurrentHashMap<>();

   void lock(Integer lockName, Transaction txn, String requestedMode) throws InterruptedException {
       lockTable.putIfAbsent(lockName, new Lock());
       Lock lock = lockTable.get(lockName);

       if (requestedMode.equals("S") && hasLock(txn, lockName) && lock.getMode().equals("S")) {
           return;
       }
       else if (requestedMode.equals("X") && hasLock(txn, lockName) && lock.getMode().equals("X")) {
           return;
       }
       else if (requestedMode.equals("X") && hasLock(txn, lockName) && lock.getMode().equals("S")) {
           lock.upgrade();
       }
       else {
           lock.acquire(requestedMode);
       }

       txnTable.putIfAbsent(txn, new ArrayList<>());
       ArrayList<Lock> txnLockList = txnTable.get(txn);
       txnLockList.add(lock);
       txnTable.put(txn, txnLockList);
   }

   void removeTransaction(Transaction txn) {
        ArrayList<Lock> txnLockList = txnTable.get(txn);

        for (Lock lock: txnLockList) {
            lock.release();
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

   String getLockMode(Integer lockName) {
       return lockTable.get(lockName).getMode();
   }
}

