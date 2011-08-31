/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

public abstract class BasicWorker<T> implements Runnable {
  protected Callable<T> fCallable;
  protected Future<T> fFuture;
  protected T fResult;
  protected Exception fException;
  protected long fTimeout;
  protected TimeUnit fTimeUnit;
  
  private static final ExecutorService pool = Executors.newCachedThreadPool();
  
  protected abstract void finished();
 
  protected BasicWorker(Callable<T> callable) {
    this(callable, Long.MAX_VALUE, TimeUnit.SECONDS);
  }
  
  protected BasicWorker(Callable<T> callable, long timeout, TimeUnit timeUnit) {
    fCallable = callable;
    fTimeout = timeout;
    fTimeUnit = timeUnit;
  }
  
  public Exception getException() {
    return fException;
  }
  
  public boolean cancel(boolean mayInterruptIfRunning) {
    return fFuture != null && fFuture.cancel(mayInterruptIfRunning);
  }
  
  public T getResult() {
    return fResult;
  }
  
  public void run() {
    fException = null;
    try {
      fFuture = pool.submit(fCallable);
      fResult = fFuture.get(fTimeout, fTimeUnit);
    } catch(Exception e) {
      fException = e;
    } finally {
      fFuture = null;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          finished();
        }
      });
    }
  }
}
