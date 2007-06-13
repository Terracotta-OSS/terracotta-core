/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
  private StateMachineRunner runner;
  private short sessionId = (short)0xffff;

  public abstract void execute(OOOProtocolMessage msg);
  
  public void setSessionId(short id) {
    sessionId = id;
  }
  
  public short getSessionId() {
    return(sessionId);
  }
  
  public boolean matchSessionId(OOOProtocolMessage opm) {
    return(opm.getSessionId() == getSessionId());
  }

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

  public void setRunner(StateMachineRunner runner) {
    this.runner = runner;
  }
  
  public StateMachineRunner getRunner() {
    return(runner);
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

  protected final synchronized void switchToState(State state) {
    Assert.eval(state != null && isStarted());
    this.current = state;
    state.enter();
  }

  public synchronized final State getCurrentState() {
    return current;
  }

  protected abstract State initialState();

  public abstract void reset();
}