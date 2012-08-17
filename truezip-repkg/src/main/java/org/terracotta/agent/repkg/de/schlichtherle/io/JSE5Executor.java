/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * JSE5Executor.java
 *
 * Created on 18. Dezember 2006, 01:48
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

import java.util.concurrent.*;

/**
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
final class JSE5Executor implements Executor {
    private final ExecutorService service;

    /**
     * Constructs a new <code>JSE5Executor</code>.
     * This constructor is public in order to enable reflective access!
     */
    public JSE5Executor(final String threadName) {
        assert threadName != null;
        class ThreadFactory implements java.util.concurrent.ThreadFactory {
            public Thread newThread(final Runnable r) {
                final Thread t = new Thread(r, threadName);
                t.setDaemon(true);
                return t;
            }
        } // class ThreadFactory
        service = Executors.newCachedThreadPool(new ThreadFactory());
    }

    public Task submit(final Runnable target) {
        assert target != null;
        return new JSE5Task(service.submit(target));
    }

    private static final class JSE5Task implements Task {
        private final Future future;

        private JSE5Task(final Future future) {
            assert future != null;
            this.future = future;
        }

        public void cancel() {
            future.cancel(true);
            while (true) {
                try {
                    future.get();
                    break;
                } catch (CancellationException cancelled) {
                    break;
                } catch (ExecutionException readerFailure) {
                    assert false : readerFailure;
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
