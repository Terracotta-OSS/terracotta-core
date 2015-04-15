/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.util.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} that sets names to the threads created by this factory. Threads created by this factory
 * will take names in the form of the string <code>namePrefix + " thread-" + threadNum</code> where <tt>threadNum</tt> is the
 * count of threads created by this type of factory.
 * 
 */
public class NamedThreadFactory implements ThreadFactory {

    private static AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    /**
     * Constructor accepting the prefix of the threads that will be created by this {@link ThreadFactory}
     * 
     * @param namePrefix
     *            Prefix for names of threads
     */
    public NamedThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * Returns a new thread using a name as specified by this factory {@inheritDoc}
     */
    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, namePrefix + " thread-" + threadNumber.getAndIncrement());
    }

}
