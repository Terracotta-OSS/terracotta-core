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
 */
package com.tc.lang;

import com.tc.exception.ExceptionHelper;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;


import java.util.concurrent.Callable;

/**
 * A {@link ThrowableHandler} for Terracotta Client which avoids {@link System#exit(int)} on inconsistent state of
 * Terracotta Client. This handler will shutdown Terracotta Client instead through l1ShutdownCallable.
 */
public class L1ThrowableHandler implements ThrowableHandler {
  private final Callable<Void> l1ShutdownCallable;
  private final Logger logger;
  private final List<CallbackOnExitHandler> handlers = new LinkedList<>();

  public L1ThrowableHandler(Logger logger, Callable<Void> l1ShutdownCallable) {
    this.l1ShutdownCallable = l1ShutdownCallable;
    this.logger = logger;
  }

  @Override
  public synchronized void handleThrowable(Thread thread, Throwable throwable) {
    logger.error("internal error on Thread: " + thread.toString(), throwable);
    try {
      final CallbackOnExitState throwableState = new CallbackOnExitState(throwable);
      handlers.forEach(h->h.callbackOnExit(throwableState));
      l1ShutdownCallable.call();
    } catch (Exception e) {
      logger.error("internal error shutting down client", e);
    }
  }

  @Override
  public void addHelper(ExceptionHelper helper) {

  }

  @Override
  public void handlePossibleOOME(Throwable t) {

  }

  @Override
  public synchronized void addCallbackOnExitDefaultHandler(CallbackOnExitHandler callbackOnExitHandler) {
    handlers.add(callbackOnExitHandler);
  }

  @Override
  public void addCallbackOnExitExceptionHandler(Class<?> c, CallbackOnExitHandler callbackOnExitHandler) {

  }

}
