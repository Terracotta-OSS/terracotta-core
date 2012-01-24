/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.util.runtime.Os;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods to work around various NIO issues on different platforms.
 */
public class NIOWorkarounds {

  private NIOWorkarounds() {
    //
  }

  /**
   * Determine whether this IOException should be ignored on Windows. Checks for an
   * IOException("A non-blocking socket operation could not be completed immediately") as in
   * http://developer.java.sun.com/developer/bugParade/bugs/4854354.html.
   * 
   * @param ioe Exception to check
   * @return True if should be ignored on Windows
   */
  public static boolean windowsWritevWorkaround(IOException ioe) {
    final String err = ioe.getMessage();
    if (null != err) {
      // java.io.IOException can be thrown here, but it should be ignored on windows
      // http://developer.java.sun.com/developer/bugParade/bugs/4854354.html
      if (err.equals("A non-blocking socket operation could not be completed immediately")) {
        if (Os.isWindows()) { return true; }
      }
    }

    return false;
  }

  /**
   * Workaround bug in Sun VM when select() gets interrupted and throws IOException("Interrupted system call"). See Sun
   * bug 4504001 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504001)
   * 
   * @param ioe Exception to examine
   * @return True if exception should be ignored on Linux
   */
  public static boolean linuxSelectWorkaround(IOException ioe) {
    if (Os.isLinux()) {
      String msg = ioe.getMessage();
      if ("Interrupted system call".equals(msg)) { return true; }
    }

    return false;
  }

  /**
   * Workaround for select() throwing IOException("Bad file number") in Solaris running on x86 arch. Couldn't find a
   * exact bug reported on this.
   */
  public static boolean solarisOnX86SelectWorkaround(IOException ioe) {
    if (Os.isSolaris() && Os.isArchx86()) {
      String msg = ioe.getMessage();
      if ((msg != null) && msg.contains("Bad file number")) { return true; }
    }
    return false;
  }

  /**
   * Force use of poll based NIO selector on Solaris 10 to work around Sun bug 6322825. This is done by setting the
   * System property java.nio.channels.spi.SelectorProvider to "sun.nio.ch.PollSelectorProvider". The workaround is only
   * applied on Solaris 10, JDK < 1.6. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322825
   */
  public static void solaris10Workaround() {
    boolean workaround = solaris10Workaround(System.getProperties());
    if (workaround) {
      String prev = System.getProperty("java.nio.channels.spi.SelectorProvider");
      System.setProperty("java.nio.channels.spi.SelectorProvider", "sun.nio.ch.PollSelectorProvider");

      String msg = "\nWARNING: Terracotta is forcing the use of poll based NIO selector to workaround Sun bug 6322825\n";
      if (prev != null) {
        msg += "This overrides the previous value of " + prev + "\n";
      }
      System.err.println(msg);
    }
  }

  /**
   * Determine whether the Solaris 10 workaround should be applied. see LKC-2436 and
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322825
   * 
   * @return true if the workaround should be applied
   */
  static boolean solaris10Workaround(Properties props) {
    String vendor = props.getProperty("java.vendor", "");
    String osName = props.getProperty("os.name", "");
    String osVersion = props.getProperty("os.version", "");
    String javaVersion = props.getProperty("java.version", "");

    if (vendor.toLowerCase().startsWith("sun") && "SunOS".equals(osName) && "5.10".equals(osVersion)) {
      if (javaVersion.matches("^1\\.[12345]\\..*")) { // bug is fixed in 1.6+ (supposedly)
        // Bug is fixed in 1.5.0_08+ (again, supposedly)
        Pattern p = Pattern.compile("^1\\.5\\.0_(\\d\\d)$");
        Matcher m = p.matcher(javaVersion);
        if (m.matches()) {
          String minorRev = m.group(1);
          int ver = Integer.parseInt(minorRev);
          if (ver >= 8) { return false; }
        }

        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the NPE should be ignored due to bug 6427854. See
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6427854
   * 
   * @param npe Exception to examine
   * @return True if exception should be ignored
   */
  public static boolean selectorOpenRace(NullPointerException npe) {
    StackTraceElement source = npe.getStackTrace()[0];
    if (source.getClassName().equals("sun.nio.ch.Util") && source.getMethodName().equals("atBugLevel")) { return true; }
    return false;
  }

  /**
   * Apply Solaris 10 workaround if applicable.
   * 
   * @param args Ignored
   */
  public static void main(String args[]) {
    NIOWorkarounds.solaris10Workaround();
  }

  /**
   * Determine whether to retry during connect
   * 
   * @param cse Exception to examine
   * @return True if should retry
   */
  public static boolean connectWorkaround(ClosedSelectorException cse) {
    // see DEV-671, DEV-2337

    // it isn't 100% sure, but this sun bug looks like it is the culprit requiring this workaround:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6645197

    StackTraceElement[] stackTrace = cse.getStackTrace();
    if (stackTrace.length < 3) { return false; }

    StackTraceElement f1 = stackTrace[0];
    StackTraceElement f2 = stackTrace[1];
    StackTraceElement f3 = stackTrace[2];

    return f1.getClassName().equals("sun.nio.ch.SelectorImpl") && f1.getMethodName().equals("lockAndDoSelect")
           && f2.getClassName().equals("sun.nio.ch.SelectorImpl") && f2.getMethodName().equals("selectNow")
           && f3.getClassName().equals("sun.nio.ch.Util") && f3.getMethodName().equals("releaseTemporarySelector");
  }

}
