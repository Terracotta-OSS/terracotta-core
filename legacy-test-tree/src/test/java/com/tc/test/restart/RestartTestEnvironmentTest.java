/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.restart;

import com.tc.test.TCTestCase;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RestartTestEnvironmentTest extends TCTestCase {

  public void testChoosePort() throws Exception {
    PortChooser portChooser = new PortChooser();
    RestartTestEnvironment env = new RestartTestEnvironment(getTempDirectory(), portChooser, RestartTestEnvironment.PROD_MODE);
    int port = env.chooseAdminPort();
    NoExceptionLinkedQueue control = new NoExceptionLinkedQueue();
    System.err.println("I chose port " + port);
    Listener listener = new Listener(port, control);
    listener.start();
    control.take();
    Socket s = new Socket("localhost", port);
    int value = 10;
    s.getOutputStream().write(value);
    assertEquals(new Integer(value), control.take());
  }

  private static final class Listener extends Thread {
    private int                          port;
    private final NoExceptionLinkedQueue control;

    public Listener(int port, NoExceptionLinkedQueue control) {
      this.port = port;
      this.control = control;
    }

    public void run() {
      try {
        ServerSocket s = new ServerSocket(port);
        control.put(new Object());
        Socket socket = s.accept();
        InputStream in = socket.getInputStream();
        control.put(new Integer(in.read()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
