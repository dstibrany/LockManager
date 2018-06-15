# LockManager

- Requests come in to the lock manager to grab read or write locks on objects for specific transactions
- keep track of which locks a txn has
- keep track of which txns hold locks on an object
- any number of txns can hold a read lock on an object
- only one txn can hold a write lock on an object
- read and write locks conflict
- a txn blocks when trying to acquire a lock that is in use
- locks wait and are released in FIFO order
