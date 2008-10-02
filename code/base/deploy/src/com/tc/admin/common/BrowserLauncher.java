/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.lang.reflect.Method;

import javax.swing.JOptionPane;

public class BrowserLauncher {
  private static final String   errMsg        = "Error attempting to launch web browser";

  private static final String[] UNIX_BROWSERS = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

  public static void openURL(String url) {
    String osName = System.getProperty("os.name");

    try {
      if (osName.startsWith("Mac OS")) {
        Class fileMgr = Class.forName("com.apple.eio.FileManager");
        Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });

        openURL.invoke(null, new Object[] { url });
      } else if (osName.startsWith("Windows")) {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
      } else {
        String browser = null;

        for (int count = 0; count < UNIX_BROWSERS.length && browser == null; count++) {
          if (Runtime.getRuntime().exec(new String[] { "which", UNIX_BROWSERS[count] }).waitFor() == 0) {
            browser = UNIX_BROWSERS[count];
          }
        }

        if (browser == null) {
          throw new Exception("Could not find web browser");
        } else {
          Runtime.getRuntime().exec(new String[] { browser, url });
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, errMsg + ":\n" + e.getLocalizedMessage());
    }
  }
}
