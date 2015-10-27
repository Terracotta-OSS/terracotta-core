/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.object;

import com.tc.exception.TCNotRunningException;
import com.tc.util.Assert;
import com.tc.util.Util;

/**
 * @author vmad
 */
public class ClientEntityStateManager {

    public ClientEntityStateManager() {
        this.currentState = State.RUNNING;
    }

    public synchronized void start() {
        moveTo(State.STARTING);
    }

    public synchronized void stop() {
        moveTo(State.STOPPED);
    }

    public synchronized void running() {
        moveTo(State.RUNNING);
    }

    public synchronized void pause() {
        moveTo(State.PAUSED);
    }

    public synchronized void waitUntilRunning() {
        boolean isInterrupted = false;
        try {
            while (State.RUNNING != this.currentState) {
                if (State.STOPPED == this.currentState) {
                    throw new TCNotRunningException();
                }
                try {
                    wait();
                } catch (final InterruptedException e) {
                    isInterrupted = true;
                }
            }
        } finally {
            Util.selfInterruptIfNeeded(isInterrupted);
        }
    }

    public String getCurrentState() {
        return currentState.toString();
    }

    private enum State {
        PAUSED {
            @Override
            void check(State newState) {
                Assert.assertTrue("Attempt to pause while PAUSED", newState != PAUSED);
            }
        }, RUNNING {
            @Override
            void check(State newState) {
                Assert.assertTrue("Attempt to unpause while RUNNING", newState != RUNNING);
                Assert.assertTrue("Attempt to handshake while RUNNING", newState != STARTING);
            }
        }, STARTING {
            @Override
            void check(State newState) {
                Assert.assertTrue("Attempt to handshake while STARTING", newState != STARTING);
            }
        }, STOPPED {
            @Override
            void check(State newState) {
                //does nothing for now
            }
        };


        abstract void check(State newState);

    }

    private void moveTo(State newState) {
        currentState.check(newState);
        currentState = newState;
        notifyAll();
    }


    private volatile State currentState;

}
