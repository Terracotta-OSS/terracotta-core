/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.cflow;

import java.util.Stack;

/**
 * An abstraction for the JIT gen cflow aspects.
 * <p/>
 * A concrete JIT gen cflow aspect *class* will be generated per
 * cflow sub expression with a consistent naming scheme aka cflowID.
 * <p/>
 * The concrete cflow class will extends this one and implements two static methods.
 * See the sample nested class.
 * <p/>
 * Note: the Cflow implements a real aspectOf singleton scheme and is not visible to Aspects.aspectOf
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public abstract class AbstractCflowSystemAspect {

  //TODO do we really need a stack ? I think that an int increment wrapped in a ThreadLocal
  // will be ok. The stack might only be needed for perCflow deployments
  public ThreadLocal m_cflowStackLocal = new ThreadLocal() {
    protected Object initialValue() {
      return new Stack();
    }
  };

  /**
   * before advice when entering this cflow
   */
  public void enter() {
    ((Stack) m_cflowStackLocal.get()).push(Boolean.TRUE);
  }

  /**
   * after finally advice when exiting this cflow
   */
  public void exit() {
    ((Stack) m_cflowStackLocal.get()).pop();
  }

  /**
   * @return true if in the cflow
   */
  public boolean inCflow() {
    return ((Stack) m_cflowStackLocal.get()).size() > 0;
  }

  /**
   * Sample jit cflow aspect that will gets generated.
   * Note that we need to test the INSTANCE in case the cflow subexpression
   * was out of the scope of the weaver (else we gets NullPointerExceptions)
   *
   * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
   */
  private static class Cflow_sample extends AbstractCflowSystemAspect {

    private static Cflow_sample INSTANCE = null;

    private Cflow_sample() {
      super();
    }

    /**
     * this method will be invoked by the JIT joinpoint
     */
    public static boolean isInCflow() {
      if (INSTANCE == null) {
        return false;
      }
      return INSTANCE.inCflow();
    }

    /**
     * Real aspectOf as a singleton
     */
    public static Cflow_sample aspectOf() {
      if (INSTANCE == null) {
        INSTANCE = new Cflow_sample();
      }
      return INSTANCE;
    }

  }

}
