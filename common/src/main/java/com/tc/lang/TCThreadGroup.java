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
package com.tc.lang;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.util.runtime.ThreadDumpUtil;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCThreadGroup extends ThreadGroup {

  private final ThrowableHandler throwableHandler;
  private final boolean stoppable;

  private static final Logger LOGGER = LoggerFactory.getLogger(TCThreadGroup.class);

  public TCThreadGroup(ThrowableHandler throwableHandler) {
    this(throwableHandler, "TC Thread Group");
  }
  
  public TCThreadGroup(ThrowableHandler throwableHandler, String name) {
    this(throwableHandler, name, true);
  }

  public TCThreadGroup(ThrowableHandler throwableHandler, String name, boolean stoppable) {
    super(name);
    this.throwableHandler = throwableHandler;
    this.stoppable = stoppable;
  }

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    throwableHandler.handleThrowable(thread, throwable);
  }

  public void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitDefaultHandler(callbackOnExitHandler);
  }
  
  public void addCallbackOnExitExceptionHandler(Class<?> c, CallbackOnExitHandler callbackOnExitHandler) {
    throwableHandler.addCallbackOnExitExceptionHandler(c, callbackOnExitHandler);
  }

  public boolean isStoppable() {
    return stoppable;
  }

  public void printLiveThreads(Consumer<String> reporter) {
    for (Thread t : threads()) {
      if (t != null && t != Thread.currentThread()) {
        reporter.accept(ThreadDumpUtil.getThreadDump(t));
      }
    }
  }

  public void interruptThreads() {
    threads().stream().filter(t -> t != Thread.currentThread() && t.isAlive()).forEach(Thread::interrupt);
  }

  public boolean retire(long timeout, Consumer<InterruptedException> interruptHandler) {
    boolean complete = false;
    long killStart = System.currentTimeMillis();
    if (stoppable) {
      while (!complete && System.currentTimeMillis() < killStart + timeout) {
        complete = true;
        for (Thread t : threads()) {
          if (t != Thread.currentThread()) {
            try {
              t.join(1000);
            } catch (InterruptedException i) {
              interruptHandler.accept(i);
            }
            complete = complete && !t.isAlive();
          }
        }
      }
      if (activeCount() == 0) {
        destroy();
      }
    }
    if (complete) {
      LOGGER.info("finished thread exiting in {} seconds", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - killStart));
    }
    return complete;
  }

  private synchronized List<Thread> threads() {
    int ac = activeCount();
    Thread[] list = new Thread[ac];
    enumerate(list, true);
    return Arrays.stream(list).filter(t -> t != null && t.isAlive()).collect(Collectors.toList());
  }
}
