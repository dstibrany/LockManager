# LockManager

This is a generic Lock Manager that can be used to implement Two-phase locking (2PL). It can be embedded in another system, like a relational database, as a standalone library.

## Assumptions
This library relies on the assumption that each transaction is executed in its own thread. This library is not thread-safe if a specific transaction is run across multiple threads.

## Concurrency
The level of lock granularity used is that of the specific object being locked, as opposed to locking a specific data structure. That is to say, there will be no lock contention if two distinct objects are being locked by multiple transactions. Additionally, read throughput is kept high by ensuring that an unlimited numbers of transactions can hold shared (read) locks on an object. However, readers do block writers, and writers do block readers, so only one transaction can hold an exclusive (write) lock on a specific object.

## Deadlock Detection
A wait-for graph is used to track the blocking relationship between transactions. For example, suppose transaction 1 holds an exclusive lock on object A, and transaction 2 tries to acquire a shared (or exclusive) lock on object A. The wait-for graph will then contain two nodes, which represent the two transactions, and a directed edge between node 2 and node 1, since transaction 2 is blocked by transaction 1. 

When a transaction is blocked trying to acquire a lock, a deadlock detection algorithm is run, which attempts to find a cycle in the wait-for graph. If a cycle is found, the transaction that caused the deadlock is aborted.

## Other features
- Locks can be upgraded. If a transaction holds a shared lock, it can be upgraded to an exclusive lock without needing to release the shared lock.
- Locks are acquired in FIFO order.
- Barging is prevented. That is, a transaction waiting on an exclusive lock will not be indefinitely prevented from acquiring the lock by a constant stream of read lock requests.
- Thorough test suite including concurrent tests for mutual exclusion and deadlock detection.

## API  

```
void lock(int lockName, int txnId, Lock.LockMode requestedMode);
```
Acquire a lock on behalf of a transaction. Hashcodes should be used to represent `lockName` and `txnId`.

`requestedMode` can be one of `Lock.LockMode.SHARED`, `Lock.LockMode.EXCLUSIVE` 

```
void unlock(int lockName, int txnId);
```
Release a lock behalf of a transaction. Hashcodes should be used to represent `lockName` and `txnId`.

```
void removeTransaction(int txnId);
```
Release all locks associated with a transaction. A hashcode should be used to represent `txnId`.

```
boolean hasLock(int txnId, int lockName);
```
Check if a lock is held on behalf of a transaction. Hashcodes should be used to represent `lockName` and `txnId`.

## Building

To build:
```
gradle build
```
To test:
```
gradle test
```
To release:
```
gradle jar
```

