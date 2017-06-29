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
package com.tc.async.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.Stage;
import com.tc.exception.TCNotRunningException;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.util.concurrent.QueueFactory;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TCNotRunningTest extends TestCase {

  private StageManagerImpl          stageManager;
  private TestHandler<TestEventContext>               testHandler;
  private TestCallbackOnExitHandler callbackOnExitHandler;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    debug("In setup");
    try {
      ThrowableHandler throwableHandler = new NonExitingThrowableHandler(LoggerFactory.getLogger(StageManagerImpl.class));
      stageManager = new StageManagerImpl(new TCThreadGroup(throwableHandler), new QueueFactory<TestEventContext>());
      callbackOnExitHandler = new TestCallbackOnExitHandler();
      throwableHandler.addCallbackOnExitDefaultHandler(callbackOnExitHandler);
      testHandler = new TestHandler<TestEventContext>();
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void testDirect() {
    debug("Running direct test");
    Stage<TestEventContext> stage = stageManager.createStage("some-stage", TestEventContext.class, testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().addSingleThreaded(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    Assert.assertFalse("Exit should not be called", callbackOnExitHandler.exitCalled);
    debug("test complete");
  }

  public void testWrapped() {
    debug("Testing wrapped exception");
    testHandler.state = HandlerState.WRAPPED_EXCEPTION;
    Stage<TestEventContext> stage = stageManager.createStage("some-stage-2", TestEventContext.class, testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().addSingleThreaded(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    Assert.assertFalse("Exit should not be called", callbackOnExitHandler.exitCalled);
    debug("test complete");
  }

  public void testOtherException() {
    debug("Testing other exception");
    testHandler.state = HandlerState.OTHER_EXCEPTION;
    Stage<TestEventContext> stage = stageManager.createStage("some-stage-3", TestEventContext.class, testHandler, 1, 10);
    stage.start(new ConfigurationContextImpl(null));
    stage.getSink().addSingleThreaded(new TestEventContext());
    testHandler.waitUntilHandledEventCount(1);
    callbackOnExitHandler.waitUntilExitCalled();
    debug("test complete");
  }

  private static void debug(String msg) {
    System.out.println("[" + System.currentTimeMillis() + "]" + msg);
  }

  private static enum HandlerState {
    DIRECT_EXCEPTION, WRAPPED_EXCEPTION, OTHER_EXCEPTION;
  }

  private static class TestHandler<EC> extends AbstractEventHandler<EC> {

    private volatile HandlerState state             = HandlerState.DIRECT_EXCEPTION;
    private final AtomicInteger   handledEventCount = new AtomicInteger();

    @Override
    public void handleEvent(EC context) {
      try {
        switch (state) {
          case DIRECT_EXCEPTION:
            throw new TCNotRunningException("Direct exception");
          case WRAPPED_EXCEPTION:
            throw new RuntimeException(new TCNotRunningException("Direct exception"));
          case OTHER_EXCEPTION:
            throw new RuntimeException("Some other exception");
          default:
            throw new AssertionError(state);
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

  private static class TestEventContext {
    //
  }

  private static class TestCallbackOnExitHandler implements CallbackOnExitHandler {
    private volatile boolean exitCalled = false;

    @Override
    public void callbackOnExit(CallbackOnExitState state) {
      debug("Callback on exit called");
      exitCalled = true;
    }

    public void waitUntilExitCalled() {
      int iterations = 0;
      while (!exitCalled) {
        debug("Waiting until exit called: " + exitCalled);
        if (iterations++ >= 60) { throw new RuntimeException("Waited for a minute already. Exit not called: "
                                                             + exitCalled); }
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

  private static class NonExitingThrowableHandler extends ThrowableHandlerImpl {

    public NonExitingThrowableHandler(Logger logger) {
      super(logger);
    }

    @Override
    protected synchronized void exit(int status) {
      debug("EXIT CALLED - not exiting for tests");
    }

  }
}
