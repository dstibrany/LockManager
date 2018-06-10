package lockmanager;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

class LockManager {
    private ConcurrentHashMap<Integer, Lock> lockTable = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, ArrayList<Lock>> txnTable = new ConcurrentHashMap<>();

   void lock(Integer lockName, Integer txnId, String mode) {
       lockTable.putIfAbsent(lockName, new Lock());
       Lock lock = lockTable.get(lockName);

       lock.acquire(mode);

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
}

