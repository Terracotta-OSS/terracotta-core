/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

import com.tc.exception.TCInternalError;

import java.io.Serializable;

/**
 * A class to model a flag that can be set once and only once If you have a boolean in class that should
 * really only be set once and only once (like a shutdown flag maybe), using this class might help your class stay
 * implemented correctly <br>
 * <br>
 * NOTE: There is purposefully no way to reset the flag. Use of this class is meant to communicate that a particular
 * flag should be set once and only once (period). <br>
 * NOTE(2): This class is a lot like a latch in some ways (ie. it can only be set once and not reset). Unlike a latch,
 * the set() method here is only allowed to be called once (ever). Additionally, there are no wait() style methods on
 * this flag class
 *
 * @author teck
 */
public final class SetOnceFlag implements Serializable {
  // XXX: review Serialization semantics. Is it possible to get a SetOnceFlag that can be
  //      reset (ie. via serialize/deserialize)

  private volatile boolean set;

  public SetOnceFlag() {
    this(false);
  }

  /**
   * Create a new SetOnceFlag, optionally <code>set()</code> 'ing the flag immediately
   *
   * @param setFlag true if the flag should be created already <code>set()</code>
   */
  public SetOnceFlag(final boolean setFlag) {
    if (setFlag) {
      this.set = true;
    }
  }

  /**
   * Attempt to set the flag
   *
   * @return true if the flag was set, false otherwise
   * @throws IllegalArgumentException if the value has already been set by a different thread
   */
  public void set() {
    synchronized (this) {
      if (set) {
        throw new IllegalStateException("Flag has already been set");
      } else {
        set = true;
      }
    }
  }

  /**
   * Attempt to atomically set the flag. This differs from <code>set()</set> in that
   * it doesn't throw an exception if the value has already been set. This method
   * is useful for flags that might be set more than once, but should act as a guard
   * against path(s) that should only ever run once.
   *
   * NOTE: It is probably almost always wrong to ignore the return value of this method
   *
   * @return true iff this invocation actually set the flag, false otherwise
   */
  public boolean attemptSet() {
    try {
      synchronized (this) {
        if (set) { return false; }

        set();

        return set;
      }
    } catch (Throwable t) {
      // this REALLY shouldn't happen, this would be a programming error
      throw new TCInternalError(t);
    }
  }

  /**
   * Has the flag been already set? README: This is example of how <b>NOT </b> to use this class: <br>
   *
   * <pre>
   * if (!flag.isSet()) {
   *   flag.set();
   *
   *   // ... do some protected, assumed one-time actions
   * }
   * </pre>
   *
   * This example code is broken becuase there is no synchronization and/or checking the return value of
   * <code>set()</code>. It is certainly possible that two threads could execute the body of the <code>if</code>
   * block A correct implementation might be: <br>
   *
   * <pre>
   * if (flag.attemptSet()) {
   *   // do one time only actions
   * } else {
   *   // flag was already set
   * }
   * </pre>
   *
   * @return true if the flag is already set
   */
  public boolean isSet() {
    return set;
  }

  public String toString() {
    return "set: " + set;
  }

}