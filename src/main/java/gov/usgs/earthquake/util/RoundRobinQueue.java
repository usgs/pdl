package gov.usgs.earthquake.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * An abstract base class for round-robin queueing.
 * 
 * Sub classes should implement the {@link #getQueueId(Object)} to control how
 * objects are added to queues.
 * 
 * @param <T>
 *            type of object being queued.
 */
public class RoundRobinQueue<T> implements Queue<T> {

	/**
	 * Map of queues, keyed by queue id (as determined by
	 * {@link #getQueueId(Object)}).
	 */
	private final HashMap<String, LinkedList<T>> queueMap = new HashMap<String, LinkedList<T>>();

	/**
	 * List of queues, same as in map, in round-robin order.
	 */
	private final LinkedList<LinkedList<T>> queueList = new LinkedList<LinkedList<T>>();

	/** Default constructor. */
	public RoundRobinQueue() {
	}

	/**
	 * This method determines which queue an object uses.
	 * 
	 * @param object
	 *            the object being added.
	 * @return id of the queue where object should be added.
	 */
	protected String getQueueId(T object) {
		return object.toString();
	}

	/** ===================== BLOCKING QUEUE METHODS ======================= **/

	/**
	 * Add an item to the queue.
	 * 
	 * @param e
	 *            item to add
	 * @return true if added.
	 */
	@Override
	public boolean add(T e) {
		// find queue
		String queueId = getQueueId(e);
		LinkedList<T> queue = queueMap.get(queueId);
		if (queue == null) {
			// create queue
			queue = new LinkedList<T>();
			queueMap.put(queueId, queue);
			queueList.add(queue);
		}
		// add to queue
		return queue.add(e);
	}

	/**
	 * Add an item to the queue, if possible.
	 * 
	 * @param e
	 *            item to add
	 * @return true if added, false otherwise.
	 */
	@Override
	public boolean offer(T e) {
		try {
			// this could in theory throw an unchecked exception
			return this.add(e);
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Retrieves and removes the head of this queue.
	 * 
	 * @return first element in queue.
	 * @throws NoSuchElementException
	 *             if queue is empty.
	 */
	@Override
	public T remove() {
		if (queueList.size() == 0) {
			throw new NoSuchElementException();
		}

		T next = null;
		// find queue
		LinkedList<T> nextQueue = queueList.removeFirst();
		// take first item
		next = nextQueue.removeFirst();
		// reschedule queue
		if (nextQueue.size() == 0) {
			// queue is empty, remove
			String queueId = getQueueId(next);
			queueMap.remove(queueId);
		} else {
			// move to end of round robin list
			queueList.add(nextQueue);
		}
		return next;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue. This method
	 * differs from the {@link #peek()} method only in that it throws an
	 * exception if this queue is empty.
	 * 
	 * @return the head of this queue.
	 * @throws NoSuchElementException
	 *             if this queue is empty.
	 */
	@Override
	public T element() {
		if (queueList.size() == 0) {
			throw new NoSuchElementException();
		}
		LinkedList<T> nextQueue = queueList.getFirst();
		return nextQueue.getFirst();
	}

	/**
	 * Retrieves and removes the head of this queue.
	 * 
	 * @return the head of this queue, or null if this queue is empty.
	 */
	@Override
	public T poll() {
		try {
			return remove();
		} catch (NoSuchElementException nsee) {
			return null;
		}
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, returning null if
	 * this queue is empty.
	 * 
	 * @return the head of this queue, or null if this queue is empty.
	 */
	@Override
	public T peek() {
		try {
			return element();
		} catch (NoSuchElementException nsee) {
			return null;
		}
	}

	/** ======================= COLLECTION METHODS ========================= **/

	@Override
	public boolean addAll(Collection<? extends T> c) {
		Iterator<? extends T> iter = c.iterator();
		boolean added = false;
		while (iter.hasNext()) {
			added = add(iter.next()) || added;
		}
		return added;
	}

	@Override
	public void clear() {
		queueList.clear();
		queueMap.clear();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		Iterator<?> iter = c.iterator();
		while (iter.hasNext()) {
			if (!contains(iter.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return queueList.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		Iterator<?> iter = c.iterator();
		boolean removed = false;
		while (iter.hasNext()) {
			removed = remove(iter.next()) || removed;
		}
		return removed;
	}

	@Override
	public int size() {
		int size = 0;
		Iterator<LinkedList<T>> iter = queueList.iterator();
		while (iter.hasNext()) {
			size = size + iter.next().size();
		}
		return size;
	}

	@Override
	public boolean contains(Object o) {
		try {
			@SuppressWarnings("unchecked")
			String queueId = getQueueId((T) o);
			LinkedList<T> queue = queueMap.get(queueId);
			return queue.contains(o);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean remove(Object o) {
		try {
			@SuppressWarnings("unchecked")
			String queueId = getQueueId((T) o);
			LinkedList<T> queue = queueMap.get(queueId);
			boolean removed = queue.remove(o);
			if (queue.isEmpty()) {
				queueMap.remove(queueId);
				queueList.remove(queue);
			}
			return removed;
		} catch (Exception e) {
			return false;
		}
	}

	/** ==================== HORRIBLY INEFFICIENT =========================== */

	/**
	 * Deep copy of another RoundRobinQueue.
	 * 
	 * This method is used for semi-destructive iteration methods.
	 * 
	 * NOTE: this assumes {@link #getQueueId(Object)} behaves the same for this
	 * and that.
	 * 
	 * @param that
	 */
	public RoundRobinQueue(final RoundRobinQueue<T> that) {
		Iterator<LinkedList<T>> iter = that.queueList.iterator();
		while (iter.hasNext()) {
			// copy list
			LinkedList<T> copy = new LinkedList<T>(iter.next());
			// add to round robin list
			this.queueList.add(copy);
			// add to map
			String queueId = that.getQueueId(copy.get(0));
			this.queueMap.put(queueId, copy);
		}
	}

	/**
	 * Flatten queue to a list.
	 * 
	 * Creates a copy (see {@link #RoundRobinQueue(RoundRobinQueue)}, then
	 * builds list by polling until it is empty.
	 * 
	 * @return list of all items currently in queue.
	 */
	public List<T> toList() {
		ArrayList<T> list = new ArrayList<T>(this.size());
		RoundRobinQueue<T> copy = new RoundRobinQueue<T>(this);
		while (true) {
			T next = copy.poll();
			if (next == null) {
				// done
				break;
			}
			list.add(next);
		}
		return list;
	}

	@Override
	public Iterator<T> iterator() {
		return this.toList().iterator();
	}

	@Override
	public Object[] toArray() {
		return this.toList().toArray();
	}

	@Override
	public <T2> T2[] toArray(T2[] array) {
		return this.toList().toArray(array);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		List<T> toremove = this.toList();
		toremove.removeAll(c);
		return this.removeAll(toremove);
	}

}
