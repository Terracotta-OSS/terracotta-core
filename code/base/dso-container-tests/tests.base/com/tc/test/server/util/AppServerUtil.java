/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.util;

import org.apache.commons.io.FileUtils;

import com.tc.process.HeartBeatService;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.text.Banner;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AppServerUtil {

  private static final PortChooser      pc     = new PortChooser();
  private static final TestConfigObject config = TestConfigObject.getInstance();

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
      ThreadUtil.reallySleep(5000);
      foundAlive = HeartBeatService.anyAppServerAlive();
    } while (foundAlive && System.currentTimeMillis() < timeout);

    return foundAlive;
  }

  public static void shutdownAndArchive(File from, File to) {
    shutdown();
    archive(from, to);
  }

  public static void shutdown() {
    awaitShutdown(2 * 60 * 1000);
    System.out.println("Send kill signal to app servers...");
    HeartBeatService.sendKillSignalToChildren();
  }

  public static File createSandbox(File tempDir) {
    File sandbox = null;
    if (Os.isWindows()) {
      sandbox = new File(config.cacheDir(), "sandbox");
    } else {
      sandbox = new File(tempDir, "sandbox");
    }
    
    try {
      if (sandbox.exists()) {
        if (sandbox.isDirectory()) {
          FileUtils.cleanDirectory(sandbox);
        } else {
          throw new RuntimeException(sandbox + " exists, but is not a directory");
        }
      }
    } catch (IOException e) {
      File prev = sandbox;
      sandbox = new File(sandbox.getAbsolutePath() + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
      Banner.warnBanner("Caught IOException setting up workDir as " + prev + ", using " + sandbox + " instead");
    }

    if (!sandbox.exists() && !sandbox.mkdirs()) { throw new RuntimeException("Failed to create sandbox: " + sandbox); }

    return sandbox;
  }

  public static AppServerInstallation createAppServerInstallation(NewAppServerFactory appServerFactory,
                                                                  File installDir, File sandbox) throws Exception {
    AppServerInstallation installation = null;
    String appserverHome = config.appserverHome();
    if (appserverHome != null && !appserverHome.trim().equals("")) {
      installation = appServerFactory.createInstallation(new File(appserverHome), sandbox);
    } else {
      throw new AssertionError("No appserver found! You must define: " + TestConfigObject.APP_SERVER_HOME);
    }
    return installation;
  }

  public static void archive(File from, File to) {
    if (!from.equals(to)) {
      System.out.println("Copying files from " + from + " to " + to);
      try {
        com.tc.util.io.FileUtils.copyFile(from, to);
      } catch (IOException ioe) {
        Banner.warnBanner("IOException caught while copying workingDir files");
        ioe.printStackTrace();
      }
    }    
  }
}
