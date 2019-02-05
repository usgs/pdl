package gov.usgs.earthquake.util;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Round Robin Blocking Queue.
 * 
 * {@link #put(Object)} and {@link #take()} are recommended, as other methods
 * internally call these methods.
 * 
 * @param <T> queue item type.
 */
public class RoundRobinBlockingQueue<T> extends RoundRobinQueue<T> implements
		BlockingQueue<T> {

	private final ReentrantLock changeLock;
	private final Condition notEmptyCondition;

	public RoundRobinBlockingQueue() {
		changeLock = new ReentrantLock();
		notEmptyCondition = changeLock.newCondition();
	}

	/**
	 * Add an item to the queue.
	 */
	@Override
	public boolean add(T e) {
		try {
			changeLock.lockInterruptibly();
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
		try {
			super.add(e);
			notEmptyCondition.signal();
		} finally {
			changeLock.unlock();
		}
		return true;
	}

	/**
	 * Check if queue contains an item.
	 */
	@Override
	public boolean contains(Object e) {
		try {
			changeLock.lockInterruptibly();
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
		try {
			return super.contains(e);
		} finally {
			changeLock.unlock();
		}
	}

	/**
	 * Offer an item to the queue.
	 * 
	 * Calls {@link #add(Object)}, but returns false if any exceptions thrown.
	 */
	@Override
	public boolean offer(T e) {
		try {
			return add(e);
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Offer an item to the queue.
	 * 
	 * Same as {@link #offer(Object)}, this is an unbounded queue.
	 */
	@Override
	public boolean offer(T e, long timeout, TimeUnit unit)
			throws InterruptedException {
		changeLock.tryLock(timeout, unit);
		try {
			super.add(e);
			notEmptyCondition.signal();
		} finally {
			changeLock.unlock();
		}
		return true;
	}

	/**
	 * Retrieves and removes the head of this queue, waiting up to the specified
	 * wait time if necessary for an element to become available.
	 */
	@Override
	public T poll(long timeout, TimeUnit unit) throws InterruptedException {
		changeLock.lockInterruptibly();
		try {
			if (isEmpty()) {
				notEmptyCondition.await(timeout, unit);
			}
			try {
				return remove();
			} catch (Exception e) {
				return null;
			}
		} finally {
			changeLock.unlock();
		}
	}

	/**
	 * Put an item in the queue.
	 * 
	 * @throws InterruptedException
	 *             if interrupted while acquiring lock.
	 */
	@Override
	public void put(T e) throws InterruptedException {
		changeLock.lockInterruptibly();
		try {
			super.add(e);
			notEmptyCondition.signal();
		} catch (RuntimeException re) {
			// may be thrown by add if interrupted
			if (re.getCause() instanceof InterruptedException) {
				throw (InterruptedException) re.getCause();
			}
		} finally {
			changeLock.unlock();
		}
	}

	/**
	 * Unbounded queues return Integer.MAX_VALUE.
	 * 
	 * @return Integer.MAX_VALUE;
	 */
	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Remove an object from the queue.
	 */
	@Override
	public boolean remove(Object e) {
		try {
			changeLock.lockInterruptibly();
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
		try {
			return super.remove(e);
		} finally {
			changeLock.unlock();
		}
	}

	/**
	 * Remove an object from the queue.
	 */
	@Override
	public T take() throws InterruptedException {
		changeLock.lockInterruptibly();
		try {
			while (isEmpty()) {
				notEmptyCondition.await();
			}
			return super.remove();
		} finally {
			changeLock.unlock();
		}
	}

	/**
	 * Empty queue into a collection.
	 */
	@Override
	public int drainTo(Collection<? super T> c) {
		return drainTo(c, -1);
	}

	/**
	 * Empty queue into a collection, stopping after max elements.
	 */
	@Override
	public int drainTo(Collection<? super T> c, int max) {
		try {
			changeLock.lockInterruptibly();
		} catch (InterruptedException e) {
			// none drained
			return 0;
		}
		try {
			int count = 0;
			while (!isEmpty() && (max < 0 || count < max)) {
				c.add(remove());
				count++;
			}
			return count;
		} finally {
			changeLock.unlock();
		}
	}

}
