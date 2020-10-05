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
package com.tc.util.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.exception.TCInternalError;
import com.tc.util.TCTimeoutException;

/**
 * Class to hold "future" results. This class inspired by <code>FutureResult</code> from util.concurrent, but without
 * mandating the use of Runnable and/or Callable interfaces
 * 
 * @author teck
 */
public class TCFuture {
  private static final Logger logger = LoggerFactory.getLogger(TCFuture.class);
  private volatile boolean      set;
  private volatile boolean      cancel;
  private volatile boolean      timedOut;
  private volatile Throwable    exception;
  private volatile Object       value;
  private final Object lock;

  public TCFuture() {
    this(null);
  }
  
  public TCFuture(Object lock) {
    this.lock = lock == null ? this : lock;
    cancel = false;
    value = null;
    exception = null;
    set = false;
  }

  /**
   * Get the value of this future result, potentially blocking indefinitely until it is avaiable
   * 
   * @return the value set in this future (which may be null)
   * @throws InterruptedException if the current thread is interrupted while waiting for the result to be set
   */
  public Object get() throws InterruptedException, TCExceptionResultException {
    try {
      return get(0);
    } catch (TCTimeoutException e) {
      throw new TCInternalError("Timeout not supposed to happen here");
    }
  }

  /**
   * Get the value of this future result within the scope of the given timeout
   * 
   * @param timeout time (in milliseconds) to wait before throwing a timeout exception. A value of zero will cause this
   *        method to wait indefinitely and a timeout exception will never be thrown
   * @return the value set in this future (which may be null)
   * @throws InterruptedException if the current thread is interrupted while waiting for the result to be set
   * @throws TCTimeoutException if timeout period expires
   * @throws TCExceptionResultException if another thread sets the future result to an exception.
   * @see setException(Throwable t)
   */
  public Object get(long timeout) throws InterruptedException, TCTimeoutException, TCExceptionResultException {
    return get(timeout, true);
  }

  /**
   * Get the value of this future result within the scope of the given timeout
   * 
   * @param timeout time (in milliseconds) to wait before throwing a timeout exception. A value of zero will cause this
   *        method to wait indefinitely and a timeout exception will never be thrown
   * @param flagIfTimedOut if set to true and a TCTimeoutException is thrown waiting for the result the timedOut()
   *        method will return true, otherwise it will continue to return false
   * @return the value set in this future (which may be null)
   * @throws InterruptedException if the current thread is interrupted while waiting for the result to be set
   * @throws TCTimeoutException if timeout period expires
   * @throws TCExceptionResultException if another thread sets the future result to an exception.
   * @see setException(Throwable t)
   */
  public Object get(long timeout, boolean flagIfTimedOut) throws InterruptedException, TCTimeoutException,
      TCExceptionResultException {
    synchronized (this.lock) {
      if (cancel) {
        throw new InterruptedException("Future already cancelled"); 
      }
      long internalTimeout = timeout;
      while (!set) {
        if (internalTimeout < 0 ) {
          this.timedOut = flagIfTimedOut;
          throw new TCTimeoutException("Timeout of " + timeout + " milliseconds occurred");
        }
        long startTime = System.currentTimeMillis();
        lock.wait(internalTimeout);
        long waitTime = System.currentTimeMillis() - startTime;
        if (cancel) {
          throw new InterruptedException("Future was cancelled while waiting");
        }
        
        if (timeout != 0L) {
          //In case the timeout value is greater than zero, then only reduce the wait time.
          //If timeout == 0, thread waits indefinitely (Always internalTimeout == timeout == 0L)
          internalTimeout -= waitTime;
          if (internalTimeout == 0L) {
            //If internalTimeout value has become zero, that means, total time the thread has waited
            //is exactly equals to the timeout value passed in the invocation.In that case, reduce it by
            //one so that client gets TCTimeoutException instead of going into indefinite wait call
            //in next iteration
            internalTimeout--;
          }
        }
      }
      if (exception == null) {
        return value;
      } else if (exception != null) {
        // NOTE: this won't work with JDK1.3
        throw new TCExceptionResultException(exception);
      }

      throw new TCInternalError("Neither exception nor value set");
    }
  }

  /**
   * Set the value of this future to an exception result. Thread(s) waiting for the result (in method <code>get()</code>)
   * will be awoken. If this future has been <code>cancel()</code> 'ed, setting the value will have no effect
   * 
   * @param ex the exception result for this future
   * @throws IllegalStateException if a result has already been set in this future
   */
  public void setException(Throwable ex) {
    if (ex == null) { throw new IllegalArgumentException("exception result cannot be null"); }

    synchronized (this.lock) {
      if (cancel) {
        return;
      }

      if (set) { throw new IllegalStateException("Future result already set"); }

      set = true;
      this.exception = ex;
      this.lock.notifyAll();
    }
  }

  /**
   * Set the value of this future. Thread(s) waiting for the result (in method <code>get()</code>) will be awoken. If
   * this future has been <code>cancel()</code> 'ed, setting the value will have no effect
   * 
   * @param value the value to set into this future
   * @throws IllegalStateException if the value has already been set in this future
   */
  public void set(Object value) {
    synchronized (this.lock) {

      if (cancel) {
        return;
      }

      if (set) { throw new IllegalStateException("Value already set"); }

      set = true;
      this.value = value;
      this.lock.notifyAll();
    }
  }

  /**
   * Cancel this future instance. Cancelling a future will cause any threads waiting on this future to receive an
   * interrupted exception instead of a result value. Calling <code>cancel()</code> after a value has been set does
   * not "unset" the value
   */
  public void cancel() {
    synchronized (this.lock) {
      cancel = true;
      this.lock.notifyAll();
    }
  }

  /**
   * @return true if a call to get(long,boolean) specified true as an argument and the call timed out, false otherwise.
   */
  public boolean timedOut() {
    return this.timedOut;
  }

}
