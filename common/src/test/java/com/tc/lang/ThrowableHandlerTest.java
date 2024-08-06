/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.lang;

import org.slf4j.LoggerFactory;

import com.tc.logging.CallbackOnExitHandler;
import com.tc.logging.CallbackOnExitState;
import com.tc.util.runtime.VmVersion;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import static org.junit.Assume.assumeFalse;

public class ThrowableHandlerTest extends TestCase {

  private boolean invokedCallback;

  public void testThrowableHandlerTest() {
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(ThrowableHandlerTest.class)) {

      @Override
      protected synchronized void exit(int status) {
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
    final ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(ThrowableHandlerTest.class)) {
      @Override
      protected synchronized void exit(int status) {
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
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(getClass())) {
      @Override
      protected synchronized void exit(int status) {
        exited.set(true);
      }
    };
    throwableHandler.handleThrowable(Thread.currentThread(),
                                     new IllegalStateException("The Thread Service has been terminated."));
    assertFalse(exited.get());
  }

  public void testIsThreadGroupDestroyed() throws Exception {
    final AtomicBoolean exited = new AtomicBoolean(false);
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(LoggerFactory.getLogger(getClass())) {
      @Override
      protected synchronized void exit(int status) {
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
      Thread tempThread = new Thread(threadGroup, r);      
      fail();
      assertFalse(tempThread.isAlive()); // stupid workaround to clear warning about unused allocated object
    } catch (Throwable th) {
      t = th;
    }

    StackTraceElement[] stack = t.getStackTrace();
    stack[stack.length - 1] = new StackTraceElement("javax.management.remote.generic.GenericConnectorServer$Receiver",
                                                    "run", "Foo.java", 12);
    t.setStackTrace(stack);

    throwableHandler.handleThrowable(thread, t);
    if (!new VmVersion(System.getProperties()).isIBM()) {
      assumeFalse(exited.get());  // this fails for IBM JDK
    }
  }

  private class TestCallbackOnExitHandler implements CallbackOnExitHandler {

    @Override
    public void callbackOnExit(CallbackOnExitState state) {
      invokedCallback = true;
    }
  }

}
