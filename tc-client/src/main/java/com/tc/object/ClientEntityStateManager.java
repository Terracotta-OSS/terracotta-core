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

import com.tc.util.Assert;

/**
 * @author vmad
 */
public class ClientEntityStateManager {

    public ClientEntityStateManager() {
        this.currentState = State.RUNNING;
    }
    
    public boolean isRunning() {
      return this.currentState == State.RUNNING;
    }
    
    public boolean isShutdown() {
      return this.currentState == State.STOPPED;
    }
    
    public boolean isPaused() {
      return this.currentState == State.PAUSED;
    }

    public void stop() {
        moveTo(State.STOPPED);
    }

    public void running() {
        moveTo(State.RUNNING);
    }

    public void pause() {
        moveTo(State.PAUSED);
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
    }


    private volatile State currentState;

}
