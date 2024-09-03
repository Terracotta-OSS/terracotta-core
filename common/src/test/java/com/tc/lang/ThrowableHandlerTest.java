/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogging;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class ThrowableHandlerTest extends TestCase {

  private boolean invokedCallback;

  public void testThrowableHandlerTest() {
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(ThrowableHandlerTest.class)) {

      @Override
      protected void exit(int status) {
        // do not exit in test.
      }

    };
    throwableHandler.addCallbackOnExitDefaultHandler(new TestCallbackOnExitHandler());
    try {
      throw new Exception(" force thread dump ");
    } catch (Exception e) {
      throwableHandler.handleThrowable(Thread.currentThread(), e);
      assertTrue(invokedCallback);
    }
  }

  public void testImmediatelyExitOnOOME() {
    final AtomicInteger exitCode = new AtomicInteger(-1);
    final ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(ThrowableHandlerTest.class)) {
      @Override
      protected void exit(int status) {
        exitCode.set(status);
      }
    };

    throwableHandler.handlePossibleOOME(new OutOfMemoryError());
    assertEquals(ServerExitStatus.EXITCODE_FATAL_ERROR, exitCode.get());
    exitCode.set(-1);
    throwableHandler.handlePossibleOOME(new RuntimeException(new OutOfMemoryError()));
    assertEquals(ServerExitStatus.EXITCODE_FATAL_ERROR, exitCode.get());
    exitCode.set(-1);
    throwableHandler.handlePossibleOOME(new RuntimeException());
    assertEquals(-1, exitCode.get());
  }

  private class TestCallbackOnExitHandler implements CallbackOnExitHandler {

    @Override
    public void callbackOnExit(CallbackOnExitState state) {
      invokedCallback = true;
    }
  }

}
