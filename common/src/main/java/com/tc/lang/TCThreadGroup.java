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
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TCThreadGroup extends ThreadGroup {

  private final ThrowableHandler throwableHandler;
  private final boolean stoppable;

  private static final Logger LOGGER = LoggerFactory.getLogger(TCThreadGroup.class);

  public TCThreadGroup(ThrowableHandler throwableHandler) {
    this(throwableHandler, "TC Thread Group", false);
  }
  
  public TCThreadGroup(ThrowableHandler throwableHandler, String name) {
    this(throwableHandler, name, false);
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

  public boolean retire(long timeout, BiConsumer<Logger, InterruptedException> interruptHandler) {
    boolean complete = false;
    long killStart = System.currentTimeMillis();
    if (stoppable) {
      while (!complete && System.currentTimeMillis() < killStart + TimeUnit.MINUTES.toMillis(1L)) {
        complete = true;
        int ac = activeCount();
        Thread[] list = new Thread[ac];
        enumerate(list, true);
        for (Thread t : list) {
          if (t != null && t != Thread.currentThread()) {
            t.interrupt();
            try {
              LOGGER.info("waiting for {} to exit", t.getName());
              t.join(500);
            } catch (InterruptedException i) {
              interruptHandler.accept(LOGGER, i);
            }
            complete = complete && !t.isAlive();
          }
        }
      }
      if (activeCount()==0) {
        destroy();
      }
    }
    LOGGER.info("finished thread exiting in {} seconds", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - killStart));
    return stoppable;
  }
}
