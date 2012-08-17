/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReentrantReadWriteLock.java
 *
 * Created on 25. Juli 2006, 13:39
 */
/*
 * Copyright 2006 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.agent.repkg.de.schlichtherle.io;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.terracotta.agent.repkg.de.schlichtherle.util.ThreadLocalCounter;

/**
 * Similar to <code>java.util.concurrent.locks.ReentrantReadWriteLock</code>
 * with the following differences:
 * <ul>
 * <li>This class requires J2SE 1.4 only.
 * <li>This class performs better than its overengineered colleague in JSE 1.5.
 * <li>This class provides locks which provide a different set of methods
 *     (with the same functionality in the common subset) in order to suit
 *     the particular needs of TrueZIP (see {@link ReentrantLock}).
 * </ul>
 * <p>
 * <b>Note:</b> In accordance with JSE 1.5, upgrading a read lock to a write
 * lock is not possible. Any attempt to do so will lock the current thread.
 * This is a constraint which can't be fixed properly: If this constraint
 * would not exist, two reader threads could try to upgrade from a read lock
 * to a write lock concurrently, effectively dead locking them.
 * By locking this thread immediately on any attempt to do so, this is
 * considered to be a programming error which can be easily fixed without
 * affecting any other thread.
 * <p>
 * On the other hand, it is possible to downgrade from a write lock to a
 * read lock. Please consult the JSE 1.5 Javadoc of the class
 * <code>java.util.concurrent.locks.ReentrantReadWriteLock</code>
 * for more information.
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.2
 */
final class ReentrantReadWriteLock implements ReadWriteLock {

    private static final String CLASS_NAME
            = "de/schlichtherle/io/ReentrantReadWriteLock".replace('/', '.'); // beware of code obfuscation!
    private static final Logger logger
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private final ReadLock readLock = new ReadLock();
    private final WriteLock writeLock = new WriteLock();

    private int totalWriteLockCount;
    private int totalReadLockCount;

    //
    // Methods.
    //

    /**
     * Returns the lock for reading.
     * Like its cousin in JSE 1.5, the returned lock does <em>not</em>
     * support upgrading to a write lock.
     */
    public ReentrantLock readLock() {
        return readLock;
    }

    /**
     * Returns the lock for writing.
     * Like its cousin in JSE 1.5, the returned lock <em>does</em>
     * support downgrading to a read lock.
     */
    public ReentrantLock writeLock() {
        return writeLock;
    }

    private synchronized void lockRead() {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... wait until no other writer has acquired a lock.
            while (totalWriteLockCount - threadWriteLockCount > 0) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "interrupted", ex);
                    logger.log(Level.FINE, "continuing");
                }
            }
        }
        totalReadLockCount++;
    }

    private synchronized void lockReadInterruptibly()
    throws InterruptedException {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... wait until no other writer has acquired a lock.
            while (totalWriteLockCount - threadWriteLockCount > 0) {
                wait();
            }
        }
        totalReadLockCount++;
    }

    private synchronized boolean tryLockRead() {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... assert that no other writer has acquired a lock.
            if (totalWriteLockCount - threadWriteLockCount > 0) {
                return false;
            }
        }
        totalReadLockCount++;
        return true;
    }

    private synchronized void unlockRead() {
        totalReadLockCount--;
        notifyAll();
    }

    private synchronized void lockWrite() {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... wait until no other writer and no readers have acquired a lock.
            // Commented out to mimic behaviour of JSE 1.5!
            //final int threadReadLockCount = readLock.lockCount();
            while (totalReadLockCount /*- threadReadLockCount*/ > 0
                    || totalWriteLockCount - threadWriteLockCount > 0) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    logger.log(Level.FINE, "interrupted", ex);
                    logger.log(Level.FINE, "continuing");
                }
            }
        }
        totalWriteLockCount++;
    }

    private synchronized void lockWriteInterruptibly()
    throws InterruptedException {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... wait until no other writer and no readers have acquired a lock.
            // Commented out to mimic behaviour of JSE 1.5!
            //final int threadReadLockCount = readLock.lockCount();
            while (totalReadLockCount /*- threadReadLockCount*/ > 0
                    || totalWriteLockCount - threadWriteLockCount > 0) {
                wait();
            }
        }
        totalWriteLockCount++;
    }

    private synchronized boolean tryLockWrite() {
        // The code repetetition in these methods isn't elegant, but it's
        // faster than the use of the strategy pattern and performance is
        // critical in this class.
        final int threadWriteLockCount = writeLock.lockCount();
        if (threadWriteLockCount <= 0) { // If I'm not the writer...
            // ... wait until no other writer and no readers have acquired a lock.
            // Commented out to mimic behaviour of JSE 1.5!
            //final int threadReadLockCount = readLock.lockCount();
            if (totalReadLockCount /*- threadReadLockCount*/ > 0
                    || totalWriteLockCount - threadWriteLockCount > 0) {
                return false;
            }
        }
        totalWriteLockCount++;
        return true;
    }

    private synchronized void unlockWrite() {
        totalWriteLockCount--;
        notifyAll();
    }

    //
    // Inner classes.
    //

    private static abstract class AbstractLock
            extends ThreadLocalCounter
            implements ReentrantLock {

        public final boolean isLocked() {
            return getCounter() > 0;
        }

        public final int lockCount() {
            return getCounter();
        }

        public void lock() {
            increment();
        }

        public void unlock() {
            int lockCount = getCounter();
            if (lockCount <= 0)
                throw new IllegalMonitorStateException();
            setCounter(lockCount - 1);
        }
    }

    private class ReadLock extends AbstractLock {
        public void lock() {
            lockRead();
            super.lock();
        }

        public void lockInterruptibly() throws InterruptedException {
            lockReadInterruptibly();
            super.lock();
        }

        public boolean tryLock() {
            boolean locked = tryLockRead();
            if (locked)
                super.lock();
            return locked;
        }

        public void unlock() {
            super.unlock();
            unlockRead();
        }
    }

    private class WriteLock extends AbstractLock {
        public void lock() {
            lockWrite();
            super.lock();
        }

        public void lockInterruptibly() throws InterruptedException {
            lockWriteInterruptibly();
            super.lock();
        }

        public boolean tryLock() {
            boolean locked = tryLockWrite();
            if (locked)
                super.lock();
            return locked;
        }

        public void unlock() {
            super.unlock();
            unlockWrite();
        }
    }
}
