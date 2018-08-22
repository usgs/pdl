/*
 * ObjectLockTest
 *
 * $Id$
 * $URL$
 */
package gov.usgs.util;

import org.junit.Assert;
import org.junit.Test;

public class ObjectLockTest {

	// Test runner places error here for later asserts.
	private final StringBuffer errors = new StringBuffer();

	// These variables control some behavior of the test and can be tweaked to
	// the developers satisfaction in order to prove the object lock is working
	// properly. To have a more robust test, make the difference between the
	// DELAY and the INTERVAL greater. The DELAY should always be greater than
	// the INTERVAL or we really aren't testing anything.

	// How long each thread sleeps before running again.
	private final long INTERVAL = 10L;

	// How long the checker thread gives the incrementer thread to change the
	// value. If locking is enabled, this number could be very large and the
	// incrementer thread will never be allowed to change the value.
	private final long DELAY = 25L;

	// How many times to run.
	private final int MAX_VALUE = 10;


	private ObjectLock<Integer> objectLock = new ObjectLock<Integer>();

	public static final Integer testObject1 = Integer.valueOf(1234);
	public static final Integer testObject2 = Integer.valueOf(1234);
	public static final Integer testObject3 = Integer.valueOf(1234);
	public static final Integer testObject4 = Integer.valueOf(1234);

	@Test
	public void readLockBlocksUntilWriteLockIsReleased() throws Exception {
		ReaderThread<Integer> reader1 = new ReaderThread<Integer>(testObject1,
				objectLock, 50);
		ReaderThread<Integer> reader2 = new ReaderThread<Integer>(testObject2,
				objectLock, 50);

		WriterThread<Integer> writer1 = new WriterThread<Integer>(testObject3,
				objectLock, 50);
		WriterThread<Integer> writer2 = new WriterThread<Integer>(testObject4,
				objectLock, 50);

		System.err.println("Starting threads");
		reader1.start();
		writer1.start();
		reader2.start();
		writer2.start();

		reader1.join();
		writer1.join();
		reader2.join();
		writer2.join();
	}

	public static class WriterThread<T> extends Thread {
		private T object;
		private ObjectLock<T> lock;
		private int numWrites;

		public WriterThread(T object, ObjectLock<T> lock, int numWrites) {
			this.object = object;
			this.lock = lock;
			this.numWrites = numWrites;
		}

		public void run() {
			long threadid = Thread.currentThread().getId();

			for (int i = 0; i < numWrites; i++) {
				System.err.println("thread " + threadid
						+ " acquiring write lock");
				try {
					lock.acquireWriteLock(object);
				} catch (InterruptedException e1) {
					Assert.fail("Interrupted while aqcuiring write lock");
				}
				System.err.println("thread " + threadid
						+ " acquired write lock");

				// simulate writing
				try {
					Thread.sleep((int) (Math.random() * 100.0));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.err.println("thread " + threadid
						+ " releasing write lock");
				lock.releaseWriteLock(object);
				System.err.println("thread " + threadid
						+ " released write lock");
			}
		}
	}

	public static class ReaderThread<T> extends Thread {
		private T object;
		private ObjectLock<T> lock;
		private int numWrites;

		public ReaderThread(T object, ObjectLock<T> lock, int numWrites) {
			this.object = object;
			this.lock = lock;
			this.numWrites = numWrites;
		}

		public void run() {
			long threadid = Thread.currentThread().getId();

			for (int i = 0; i < numWrites; i++) {
				System.err.println("thread " + threadid
						+ " acquiring read lock");
				try {
					lock.acquireReadLock(object);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					return;
				}
				System.err
						.println("thread " + threadid + " acquired read lock");

				// simulate reading
				try {
					Thread.sleep((int) (Math.random() * 100.0));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				System.err.println("thread " + threadid
						+ " releasing read lock");
				lock.releaseReadLock(object);
				System.err
						.println("thread " + threadid + " released read lock");
			}
		}
	}

	/**
	 * This method uses the ObjectLock class to show various timing is not
	 * relied upon for synchronization.
	 */
	@Test
	public void testWithLocks() {
		System.out.println("The below numbers should be strictly "
				+ "increasing, but need not be sequential. "
				+ "However the displayed logic statements "
				+ "should always evaluate to true.");

		runTest(true);
		Assert.assertEquals(errors.toString(), 0, errors.length());
	}

	/**
	 * This method does not use locks and demonstrates the failure of relying
	 * on timing for synchronization.
	 */
	@Test
	public void testWithoutLocks() {
		System.out.println("The below numbers should be strictly "
				+ "increasing, but need not be sequential. "
				+ "However the displayed logic statements "
				+ "should (at least periodically) evaluate to false.");

		runTest(false);
		Assert.assertTrue("Expected errors but worked cleanly.",
				errors.length() > 0);
	}

	/**
	 * This is the same code used by both testWithLocks and testWithoutLocks,
	 * they simply call this method with true/false respectively.
	 *
	 * @param useLock True if an ObjectLock should be used. False otherwise.
	 */
	protected void runTest(final boolean useLock) {
		final ObjectLock<LockObject> lock = new ObjectLock<LockObject>();
		final LockObject lockObject = new LockObject(1);

		/**
		 * The checker thread runs on regular intervals to check the current
		 * value of the lock object. During each run, it acquires a lock on the
		 * object, checks the value, waits for a decent amount of time (DELAY),
		 * and then checks the value again. After checking the value the second
		 * time, it ensures the value did not change while it held the lock.
		 * Finally it releases the lock.
		 */
		Thread checker = new Thread() {
			public void run() {

				while (lockObject.get() < MAX_VALUE) {
					// Get a lock
					if (useLock) {
						try {
							lock.acquireWriteLock(lockObject);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
					}

					// Check the current value of the lockObject
					int curValue = lockObject.get();

					// Sleep for a bit so the other timer has a chance to
					// increment
					// if the lock isn't working
					try {
						Thread.sleep(DELAY);
					} catch (InterruptedException iex) {
						iex.printStackTrace(System.err);
					}

					int newValue = lockObject.get();

					// Make sure the lockObject value hasn't changed (by the
					// increment timer)
					System.out.printf("%d == %d\n", curValue, newValue);

					if (curValue != newValue) {
						errors.append(String.format(
								"Lock object value changed in my "
										+ "slumber. Expected %d but got %d.",
								curValue, newValue));
					}

					// Release the lock
					if (useLock) {
						lock.releaseWriteLock(lockObject);
					}

					// Sleep then run again
					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException iex) {
						iex.printStackTrace(System.err);
					}
				}

			}
		};

		/**
		 * The incrementer thread runs on regular intervals to increment the
		 * current value of the lock object. During each run, it acquires a lock
		 * on the object, increments the value, then releases the lock.
		 */
		Thread incrementer = new Thread() {
			public void run() {
				while (lockObject.get() < MAX_VALUE) {
					// Get a lock
					if (useLock) {
						try {
							lock.acquireWriteLock(lockObject);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return;
						}
					}

					// Increment the lockObject value
					lockObject.increment();

					// Release the lock
					if (useLock) {
						lock.releaseWriteLock(lockObject);
					}

					try {
						Thread.sleep(INTERVAL);
					} catch (InterruptedException iex) {
						iex.printStackTrace(System.err);
					}
				}
			}
		};

		// Let the threads dual :)
		checker.start();
		incrementer.start();

		// Wait for these threads to complete
		try {
			checker.join();
			incrementer.join();
		} catch (InterruptedException iex) {
			iex.printStackTrace(System.err);
		}

	}

	/**
	 * Simple lock object. Behaves like an Integer object wrapper, but allows
	 * you to change the underlying value without re-assigning the object
	 * itself.
	 *
	 */
	protected static class LockObject {
		private int value = 0;

		public LockObject(int value) {
			this.value = value;
		}

		public void increment() {
			this.value += 1;
		}

		public int get() {
			// Just ensuring pass-by-value. Not that we need to.
			return (Integer.valueOf(this.value)).intValue();
		}
	}
}
