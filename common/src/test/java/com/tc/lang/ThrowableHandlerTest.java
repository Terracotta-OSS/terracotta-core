/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.lang;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogging;

import junit.framework.TestCase;

public class ThrowableHandlerTest extends TestCase {

  private boolean invokedCallback;

  public void testThrowableHandlerTest() {
    ThrowableHandler throwableHandler = new ThrowableHandler(TCLogging.getLogger(ThrowableHandlerTest.class)) {

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

  private class TestCallbackOnExitHandler implements CallbackOnExitHandler {

    public void callbackOnExit(CallbackOnExitState state) {
      invokedCallback = true;
    }
  }

}
