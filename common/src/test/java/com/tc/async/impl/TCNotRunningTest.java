/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TCNotRunningTest extends TestCase {

  private StageManagerImpl          stageManager;
  private TestHandler               testHandler;
  private TestCallbackOnExitHandler callbackOnExitHandler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    debug("In setup");
    try {
      ThrowableHandler throwableHandler = new NonExitingThrowableHandler(TCLogging.getLogger(StageManagerImpl.class));
      stageManager = new StageManagerImpl(new TCThreadGroup(throwableHandler), new QueueFactory());
      callbackOnExitHandler = new TestCallbackOnExitHandler();
      throwableHandler.addCallbackOnExitDefaultHandler(callbackOnExitHandler);
      testHandler = new TestHandler();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void testDirect() {
    debug("Running direct test");
    Stage stage = stageManager.createStage("some-stage", testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().add(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    Assert.assertFalse("Exit should not be called", callbackOnExitHandler.exitCalled);
    debug("test complete");
  }

  public void testWrapped() {
    debug("Testing wrapped exception");
    testHandler.state = HandlerState.WRAPPED_EXCEPTION;
    Stage stage = stageManager.createStage("some-stage-2", testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().add(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    Assert.assertFalse("Exit should not be called", callbackOnExitHandler.exitCalled);
    debug("test complete");
  }

  public void testOtherException() {
    debug("Testing other exception");
    testHandler.state = HandlerState.OTHER_EXCEPTION;
    Stage stage = stageManager.createStage("some-stage-3", testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().add(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    junit.framework.Assert.assertTrue("Exit should be called", callbackOnExitHandler.exitCalled);
    debug("test complete");
  }

  private static void debug(String msg) {
    System.out.println("[" + System.currentTimeMillis() + "]" + msg);
  }

  private static enum HandlerState {
    DIRECT_EXCEPTION, WRAPPED_EXCEPTION, OTHER_EXCEPTION;
  }

  private static class TestHandler extends AbstractEventHandler {

    private volatile HandlerState state             = HandlerState.DIRECT_EXCEPTION;
    private final AtomicInteger   handledEventCount = new AtomicInteger();

    @Override
    public void handleEvent(EventContext context) {
      try {
        switch (state) {
          case DIRECT_EXCEPTION:
            throw new TCNotRunningException("Direct exception");
          case WRAPPED_EXCEPTION:
            throw new RuntimeException(new TCNotRunningException("Direct exception"));
          case OTHER_EXCEPTION:
            throw new RuntimeException("Some other exception");
        }
      } finally {
        handledEventCount.incrementAndGet();
      }
    }

    public void waitUntilHandledEventCount(int count) {
      int iterations = 0;
      while (true) {
        int currentCount = handledEventCount.get();
        if (iterations++ >= 60) { throw new RuntimeException("Waited for a minute already. Count didn't reach: "
                                                             + count + ", current count: " + currentCount); }
        debug("Waiting until handled count reaches: " + count + ", current count: " + currentCount);
        if (currentCount == count) {
          break;
        } else {
          debug("Sleeping for 1 sec...");
          try {
            Thread.sleep(1000);
          } catch (Exception e) {
            e.printStackTrace();
          }
          debug("after sleep");
        }
      }
    }

  }

  private static class TestEventContext implements EventContext {
    //
  }

  private static class TestCallbackOnExitHandler implements CallbackOnExitHandler {
    private volatile boolean exitCalled = false;

    public void callbackOnExit(CallbackOnExitState state) {
      debug("Callback on exit called");
      exitCalled = true;
    }
  }

  private static class NonExitingThrowableHandler extends ThrowableHandler {

    public NonExitingThrowableHandler(TCLogger logger) {
      super(logger);
    }

    @Override
    protected synchronized void exit(int status) {
      debug("EXIT CALLED - not exiting for tests");
    }

  }
}
