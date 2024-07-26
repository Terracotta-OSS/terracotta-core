/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
