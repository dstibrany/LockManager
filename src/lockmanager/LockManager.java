package lockmanager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class LockManager {
    private ConcurrentHashMap<Integer, Lock> lockTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ArrayList<Lock>> txnTable = new ConcurrentHashMap<>();

   void lock(Integer lockName, Integer txnId, String mode) throws InterruptedException {
       lockTable.putIfAbsent(lockName, new Lock());
       Lock lock = lockTable.get(lockName);

       if (mode.equals("S") && hasLock(txnId, lockName) && lock.getMode().equals("S")) {
           return;
       }
       else if (mode.equals("X") && hasLock(txnId, lockName) && lock.getMode().equals("X")) {
           return;
       }
       else if (mode.equals("X") && hasLock(txnId, lockName) && lock.getMode().equals("S")) {
           lock.upgrade();
       }
       else {
           lock.acquire(mode);
       }

       txnTable.putIfAbsent(txnId, new ArrayList<>());
       ArrayList<Lock> txnLockList = txnTable.get(txnId);
       txnLockList.add(lock);
       txnTable.put(txnId, txnLockList);
   }

   void removeTransaction(Integer txnId) {
        ArrayList<Lock> txnLockList = txnTable.get(txnId);

        for (Lock lock: txnLockList) {
            lock.release();
        }

        txnTable.remove(txnId);
   }

   boolean hasLock(Integer txnId, Integer lockName) {
       ArrayList<Lock> lockList = txnTable.get(txnId);
       Lock lock = lockTable.get(lockName);

       if (lockList == null) return false;
       boolean found = false;

       for (Lock txnLock: txnTable.get(txnId)) {
           if (txnLock == lock) found = true;
       }
       return found;
   }

   String getLockMode(Integer lockName) {
       return lockTable.get(lockName).getMode();
   }
}

