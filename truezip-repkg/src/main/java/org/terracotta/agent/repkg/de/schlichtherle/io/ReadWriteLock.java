/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * ReadWriteLock.java
 *
 * Created on 25. Juli 2006, 20:14
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
 * Similar to <code>java.util.concurrent.locks.ReadWriteLock</code>,
 * but uses the simplified {@link ReentrantLock} interface.
 * 
 * 
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.2
 */
interface ReadWriteLock {
    
    /**
     * Returns the lock for reading.
     */
    ReentrantLock readLock();
    
    /**
     * Returns the lock for writing.
     */
    ReentrantLock writeLock();
}
