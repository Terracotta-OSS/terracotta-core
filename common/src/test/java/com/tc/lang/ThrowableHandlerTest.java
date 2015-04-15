/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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

  public void testHandleJMXThreadServiceTermination() throws Exception {
    final AtomicBoolean exited = new AtomicBoolean(false);
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(getClass())) {
      @Override
      protected synchronized void exit(final int status) {
        exited.set(true);
      }
    };
    throwableHandler.handleThrowable(Thread.currentThread(),
                                     new IllegalStateException("The Thread Service has been terminated."));
    assertFalse(exited.get());
  }

  public void testIsThreadGroupDestroyed() throws Exception {
    final AtomicBoolean exited = new AtomicBoolean(false);
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(getClass())) {
      @Override
      protected synchronized void exit(final int status) {
        exited.set(true);
      }
    };

    Runnable r = new Runnable() {
      @Override
      public void run() {
        //

      }
    };

    ThreadGroup threadGroup = new ThreadGroup("blah");
    Thread thread = new Thread(threadGroup, r);
    thread.start();
    thread.join();
    threadGroup.destroy();

    Throwable t = null;
    try {
      new Thread(threadGroup, r);
    } catch (Throwable th) {
      t = th;
    }

    StackTraceElement[] stack = t.getStackTrace();
    stack[stack.length - 1] = new StackTraceElement("javax.management.remote.generic.GenericConnectorServer$Receiver",
                                                    "run", "Foo.java", 12);
    t.setStackTrace(stack);

    throwableHandler.handleThrowable(thread, t);
    assertFalse(exited.get());
  }

  private class TestCallbackOnExitHandler implements CallbackOnExitHandler {

    @Override
    public void callbackOnExit(CallbackOnExitState state) {
      invokedCallback = true;
    }
  }

}
