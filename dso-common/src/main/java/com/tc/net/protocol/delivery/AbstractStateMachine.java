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
package com.tc.net.protocol.delivery;

import com.tc.util.Assert;

/**
 * 
 */
public abstract class AbstractStateMachine {
  private State   current;
  private boolean started = false;
  private boolean paused  = true;

  public abstract void execute(OOOProtocolMessage msg);

  public final synchronized boolean isStarted() {
    return started;
  }

  public final synchronized void start() {
    Assert.eval(!started);
    started = true;
    paused = true;
    switchToState(initialState());
  }

  public final synchronized void pause() {
    Assert.eval("started: " + started + ", paused: " + paused, started && !paused);
    basicPause();
    this.paused = true;
  }

  protected void basicPause() {
    // Override me
  }

  protected void basicResume() {
    // Override me
  }

  public final synchronized void resume() {
    Assert.eval("started: " + started + ", paused: " + paused, started && paused);
    this.paused = false;
    basicResume();
  }

  public final synchronized boolean isPaused() {
    return this.paused;
  }

  protected synchronized void switchToState(State state) {
    Assert.eval(state != null && isStarted());
    this.current = state;
    state.enter();
  }

  public synchronized final State getCurrentState() {
    return current;
  }

  protected abstract State initialState();

  public abstract void reset();

  @Override
  public String toString() {
    return "Started: " + isStarted() + "; Paused: " + isPaused();
  }
}
