/*
 * ObjectLock
 *
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Reentrant ReadWrite Locking per object.
 *
 * This is intended for use when multiple sections of code should allow
 * concurrent access, but only when operating on independent objects.
 *
 * @param <T>
 *            The type of object used for locking. This object is used as a key
 *            in a HashMap. Objects that are equal, but not necessarily ==,
 *            reference the same lock.
 */
public class ObjectLock<T> {

	/** map object to corresponding lock for object. */
	private HashMap<T, ReentrantReadWriteLock> locks = new HashMap<T, ReentrantReadWriteLock>();

	/**
	 * keep track of how many threads have, or are about to, use the lock object
	 * in the map 'locks'. When this count reaches zero, it is safe to remove
	 * the lock object.
	 */
	private HashMap<T, Integer> lockThreadCounts = new HashMap<T, Integer>();

	private final Object syncObject = new Object();

	/**
	 * Construct a new ObjectLock object.
	 */
	public ObjectLock() {
	}

	/**
	 * Get the lock for an object.
	 *
	 * This method must only be called by one thread at a time. synchronization
	 * is handled by other methods in this class.
	 *
	 * @param object
	 *            object to lock.
	 * @return lock corresponding to object.
	 */
	private ReentrantReadWriteLock getLock(final T object) {
		ReentrantReadWriteLock lock = locks.get(object);
		if (lock == null) {
			lock = new ReentrantReadWriteLock(true);
			locks.put(object, lock);
		}

		return lock;
	}

	/**
	 * Increment the thread count for an object.
	 *
	 * This method must only be called by one thread at a time. synchronization
	 * is handled by other methods in this class.
	 *
	 * @param object
	 */
	private void incrementThreadCount(final T object) {
		Integer threadCount = lockThreadCounts.get(object);
		if (threadCount == null) {
			threadCount = Integer.valueOf(0);
		}
		threadCount = threadCount + 1;
		lockThreadCounts.put(object, threadCount);
	}

	/**
	 * Decrement the thread count for an object. Also, when the thread count
	 * reaches zero, the lock corresponding to this object is removed from the
	 * locks map.
	 *
	 * This method must only be called by one thread at a time. synchronization
	 * is handled by other methods in this class.
	 *
	 * @param object
	 */
	private void decrementThreadCount(final T object) {
		Integer threadCount = lockThreadCounts.get(object);
		if (threadCount == null) {
			throw new IllegalStateException(
					"Trying to decrement thread count that does not exist.");
		}
		threadCount = threadCount - 1;
		lockThreadCounts.put(object, threadCount);

		if (threadCount == 0) {
			// no threads are using this lock anymore, cleanup
			locks.remove(object);
			lockThreadCounts.remove(object);
		}
	}

	/**
	 * Acquire a read lock for an object.
	 *
	 * Callers MUST subsequently call releaseReadLock.
	 *
	 * @param object
	 *            the object to lock for reading.
	 * @throws InterruptedException
	 */
	public void acquireReadLock(final T object) throws InterruptedException {
		ReentrantReadWriteLock lock = null;
		synchronized (syncObject) {
			lock = getLock(object);
			incrementThreadCount(object);
		}
		// do this outside synchronized for concurrency
		lock.readLock().lockInterruptibly();
	}

	/**
	 * Check if the calling thread currently has a write lock for the object.
	 *
	 * @param object
	 *            object to check.
	 * @return true if the current thread currently holds a write lock, false
	 *         otherwise.
	 */
	public boolean haveWriteLock(final T object) {
		ReentrantReadWriteLock lock = locks.get(object);
		if (lock == null) {
			return false;
		}
		return lock.isWriteLockedByCurrentThread();
	}

	/**
	 * Release a held read lock for an object.
	 *
	 * Callers MUST have previously called acquireReadLock(object).
	 *
	 * @param object
	 *            the object to unlock for reading.
	 */
	public void releaseReadLock(final T object) {
		synchronized (syncObject) {
			ReentrantReadWriteLock lock = getLock(object);
			decrementThreadCount(object);
			lock.readLock().unlock();
		}
	}

	/**
	 * Acquire a write lock for an object.
	 *
	 * Callers MUST also call releaseWriteLock(object).
	 *
	 * @param object
	 *            the object to lock for writing.
	 * @throws InterruptedException
	 */
	public void acquireWriteLock(final T object) throws InterruptedException {
		ReentrantReadWriteLock lock = null;
		synchronized (syncObject) {
			lock = getLock(object);
			incrementThreadCount(object);
		}
		// do this outside synchronized for concurrency
		lock.writeLock().lockInterruptibly();
	}

	/**
	 * Release a held write lock for an object.
	 *
	 * Callers MUST have previously called acquireWriteLock(object).
	 *
	 * @param object
	 *            the object to unlock for writing.
	 */
	public void releaseWriteLock(final T object) {
		synchronized (syncObject) {
			ReentrantReadWriteLock lock = getLock(object);
			decrementThreadCount(object);
			lock.writeLock().unlock();
		}
	}

	/**
	 * This is a synonym for acquireWriteLock, which is an exclusive lock for
	 * this object.
	 *
	 * @param object
	 *            the object to lock.
	 * @throws InterruptedException
	 */
	public void acquireLock(final T object) throws InterruptedException {
		acquireWriteLock(object);
	}

	/**
	 * This is a synonym for releaseWriteLock, which is an exclusive lock for
	 * this object.
	 *
	 * @param object
	 *            the object to unlock.
	 */
	public void releaseLock(final T object) {
		releaseWriteLock(object);
	}

}
