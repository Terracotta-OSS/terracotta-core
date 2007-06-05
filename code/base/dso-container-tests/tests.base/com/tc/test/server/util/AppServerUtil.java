/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.util;

import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.Socket;

public class AppServerUtil {

  private static final PortChooser pc = new PortChooser();

  public static int getPort() throws Exception {
    return pc.chooseRandomPort();
  }

  public static void waitForPort(int port, long waitTime) {
    final long timeout = System.currentTimeMillis() + waitTime;
    while (System.currentTimeMillis() < timeout) {
      Socket s = null;
      try {
        s = new Socket("127.0.0.1", port);
        return;
      } catch (IOException ioe) {
        // try again
      } finally {
        if (s != null) {
          try {
            s.close();
          } catch (IOException ioe) {
            // ignore
          }
        }
      }
      ThreadUtil.reallySleep(1000);
    }

    throw new RuntimeException("Port " + port + " cannot be reached, timeout = " + waitTime);
  }
  
  public static String getFullName(String serverName, String majorVersion, String minorVersion) {
    return serverName.toLowerCase() + "-" + majorVersion.toLowerCase() + "." + minorVersion.toLowerCase();
  }
}
