/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.core;

import com.tc.logging.TCLogging;
import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventCaller;
import com.tc.net.core.event.TCConnectionEventListener;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class TCConnectionEventsTest extends TestCase {

  // Exception in events should NOT be eaten
  public void testExceptions() {
    TCConnectionEventCaller caller = new TCConnectionEventCaller(TCLogging.getTestingLogger(getClass()));

    Listener listener = new Listener();

    List listeners = new ArrayList();
    listeners.add(listener);

    try {
      caller.fireCloseEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireConnectEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireEndOfFileEvent(listeners, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    try {
      caller.fireErrorEvent(listeners, null, null, null);
      fail();
    } catch (Exception e) {
      failIfNotMyException(e);
    }

    assertTrue(listener.close);
    assertTrue(listener.connect);
    assertTrue(listener.eof);
    assertTrue(listener.error);
  }

  private static void failIfNotMyException(Exception e) {
    Throwable cause = e.getCause();
    if (cause instanceof MyException) { return; }
    fail("unexpected exception type: " + cause);
  }

  private static class Listener implements TCConnectionEventListener {

    boolean close, connect, eof, error;

    public void closeEvent(TCConnectionEvent event) {
      close = true;
      throw new MyException();
    }

    public void connectEvent(TCConnectionEvent event) {
      connect = true;
      throw new MyException();
    }

    public void endOfFileEvent(TCConnectionEvent event) {
      eof = true;
      throw new MyException();
    }

    public void errorEvent(TCConnectionErrorEvent errorEvent) {
      error = true;
      throw new MyException();
    }
  }

  private static class MyException extends RuntimeException {
    //
  }

}
