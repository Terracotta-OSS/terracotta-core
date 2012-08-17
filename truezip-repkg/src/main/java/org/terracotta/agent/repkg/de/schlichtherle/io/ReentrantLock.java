/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReentrantLock.java
 *
 * Created on 25. Juli 2006, 17:47
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

/**
 * Similar to <code>java.util.concurrent.locks.Lock</code>,
 * but simplified and adapted to the particular needs of TrueZIP.
 * The subset of methods common to both this interface and its cousin
 * in JSE 1.5 is identical in functionality.
 * However, some other methods have been added here in order to suit
 * the particular needs of TrueZIP (see {@link ArchiveController}).
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.2
 */
interface ReentrantLock {

    /**
     * Returns <code>true</code> if and only if the current thread has
     * acquired this lock.
     */
    boolean isLocked();

    /**
     * Returns the number of times the current thread has successfully
     * acquired this lock.
     */
    int lockCount();

    /**
     * Acquires this lock by the current thread, eventually blocking.
     */
    void lock();

    /**
     * Acquires this lock by the current thread unless it is interrupted,
     * eventually blocking.
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * Acquires this lock if and only if it is available at the time of
     * invocation by the current thread and returns <code>true</code>.
     * Otherwise, If this lock is not available then <code>false</code>
     * is returned.
     */
    boolean tryLock();

    /**
     * Releases this lock.
     *
     * @throws IllegalMonitorStateException If the current thread has not
     *         acquired this lock.
     */
    void unlock() throws IllegalMonitorStateException;
}
