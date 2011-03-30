/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart;

import EDU.oswego.cs.dl.util.concurrent.FutureResult;

import com.tc.exception.TCRuntimeException;

public abstract class AbstractRestartTestApp implements RestartTestApp {

  protected static final State INIT   = new State(TestAppState.INIT);
  protected static final State START  = new State(TestAppState.START);
  protected static final State HOLDER = new State(TestAppState.HOLDER);
  protected static final State WAITER = new State(TestAppState.WAITER);
  protected static final State END    = new State(TestAppState.END);
  protected final FutureResult state  = new FutureResult();
  private Integer              id;
  protected final ThreadGroup  threadGroup;

  protected AbstractRestartTestApp(ThreadGroup threadGroup) {
    super();
    this.threadGroup = threadGroup;
  }

  public String getStateName() {
    return getState().getName();
  }

  public synchronized void setID(int i) {
    this.id = Integer.valueOf(i);
  }

  public synchronized int getID() {
    if (this.id == null) throw new AssertionError("ID is null.");
    return this.id.intValue();
  }

  public synchronized boolean isInit() {
    return getState().getName().equals(INIT.getName());
  }

  public synchronized boolean isStart() {
    return getState().getName().equals(START.getName());
  }

  public synchronized boolean isHolder() {
    return getState().getName().equals(HOLDER.getName());
  }

  public synchronized boolean isWaiter() {
    return getState().getName().equals(WAITER.getName());
  }

  public synchronized boolean isEnd() {
    return getState().getName().equals(END.getName());
  }

  private State getState() {
    try {
      return (State) this.state.get();
    } catch (Exception e) {
      throw new TCRuntimeException(e);
    }
  }

  protected synchronized void changeState(State newState) {
    this.state.set(newState);
    notifyAll();
  }

  static protected final class State {
    private final String name;

    private State(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}
