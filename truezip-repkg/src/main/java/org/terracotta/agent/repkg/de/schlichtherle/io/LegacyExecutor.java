/*
 * All content copyright (c) Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
/*
 * LegacyExecutor.java
 *
 * Created on 18. Dezember 2006, 01:52
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
 * @author Christian Schlichtherle
 * @version @version@
 * @since TrueZIP 6.4
 */
final class LegacyExecutor implements Executor {
    private final String threadName;

    /**
     * Constructs a new <code>LegacyExecutor</code>.
     * This constructor is public in order to enable reflective access
     * (currently unused)!
     */
    public LegacyExecutor(final String threadName) {
        assert threadName != null;
        this.threadName = threadName;
    }

    public Task submit(final Runnable target) {
        assert target != null;
        return new LegacyTask(target, threadName);
    }

    private static final class LegacyTask implements Task {
        private final Thread thread;

        private LegacyTask(final Runnable target, final String threadName) {
            assert target != null;
            assert threadName != null;
            thread = new Thread(target, threadName);
            thread.start();
        }

        public void cancel() {
            thread.interrupt();
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
