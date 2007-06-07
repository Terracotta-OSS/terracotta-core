/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.util;

import org.apache.commons.io.FileUtils;

import com.tc.process.HeartBeatService;
import com.tc.text.Banner;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
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
  
  public static boolean awaitShutdown(int timewait) {
    long start = System.currentTimeMillis();
    long timeout = timewait + start;
    boolean foundAlive = false;
    do {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        // ignore
      }
      foundAlive = HeartBeatService.anyAppServerAlive();
    } while (foundAlive && System.currentTimeMillis() < timeout);

    return foundAlive;
  }
  
  public static void shutdownAndArchive(File from, File to) {
    awaitShutdown(10 * 1000);
    System.out.println("Send kill signal to app servers...");
    HeartBeatService.sendKillSignalToChildren();
    
    System.err.println("Copying files from " + from + " to " + to);
    try {
      com.tc.util.io.FileUtils.copyFile(from, to);
    } catch (IOException ioe) {
      Banner.warnBanner("IOException caught while copying workingDir files");
      ioe.printStackTrace();
    }

    System.err.println("Deleting working directory files in " + from);
    try {
      FileUtils.forceDelete(from);
    } catch (IOException ioe) {
      Banner.warnBanner("IOException caught while deleting workingDir");
      // print this out, but don't fail test by re-throwing it
      ioe.printStackTrace();
    }
  }
}
