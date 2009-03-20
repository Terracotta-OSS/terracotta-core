/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import com.tc.lcp.HeartBeatServer;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;

/**
 * Unit test for {@link LinkedJavaProcess}.
 */
public class LinkedJavaProcessTest extends TCTestCase {
  private static final boolean DEBUG = false;

  public void testRunsRightCommand() throws Exception {
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain1.class.getName());
    process.start();

    // This is actually stdout...weird Java
    StreamCollector outCollector = new StreamCollector(process.getInputStream());
    // This is stderr
    StreamCollector errCollector = new StreamCollector(process.getErrorStream());

    outCollector.start();
    errCollector.start();

    process.waitFor();

    outCollector.join(30000);
    errCollector.join(30000);

    assertEquals("Ho there!", ignoreStandardWarnings(errCollector.toString()).trim());
    assertEquals("Hi there!", ignoreStandardWarnings(outCollector.toString()).trim());
  }

  private static String ignoreStandardWarnings(String input) {
    debugPrintln("*****  inputString=[" + input + "]");

    String delimiter = System.getProperty("line.separator", "\n");

    debugPrintln("*****  delimiter=[" + delimiter + "]");

    String[] output = input.split(delimiter);

    StringBuffer out = new StringBuffer();
    for (int i = 0; i < output.length; ++i) {
      debugPrintln("*****  piece=[" + output[i] + "]");

      if (output[i].startsWith("DATA: ")) {
        out.append(output[i].substring("DATA: ".length()) + delimiter);
        debugPrintln("***** appending [" + output[i].substring("DATA: ".length()) + delimiter + "] to output string");
      }
    }

    debugPrintln("*****  outString=[" + out.toString() + "]");

    return out.toString();
  }

  public void testIO() throws Exception {
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain2.class.getName());
    process.start();

    StreamCollector outCollector = new StreamCollector(process.getInputStream()); // stdout
    StreamCollector errCollector = new StreamCollector(process.getErrorStream()); // stderr

    outCollector.start();
    errCollector.start();

    process.getOutputStream().write("Test Input!\n".getBytes());
    process.getOutputStream().flush();

    process.waitFor();

    outCollector.join(30000);
    errCollector.join(30000);

    assertEquals("out: <Test Input!>", ignoreStandardWarnings(outCollector.toString()).trim());
    assertEquals("err: <Test Input!>", ignoreStandardWarnings(errCollector.toString()).trim());
  }

  public void testExitCode() throws Exception {
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain3.class.getName());
    process.start();

    process.waitFor();
    assertEquals(57, process.exitValue());
  }

  public void testSetup() throws Exception {
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain4.class.getName());

    File dir = getTempFile("mydir");
    assertTrue(dir.mkdirs());
    String pwd = dir.getCanonicalPath();
    process.setDirectory(dir);
    process.setEnvironment(new String[] { "LD_LIBRARY_PATH=myenv" });
    process.setJavaArguments(new String[] { "-Dljpt.foo=myprop" });

    process.start();

    StreamCollector outCollector = new StreamCollector(process.getInputStream()); // stdout
    StreamCollector errCollector = new StreamCollector(process.getErrorStream()); // stderr

    outCollector.start();
    errCollector.start();

    process.waitFor();

    outCollector.join(30000);
    errCollector.join(30000);

    String output = outCollector.toString();
    String err = errCollector.toString();

    assertEquals("", ignoreStandardWarnings(err).trim());

    assertContains("ljpt.foo=myprop", output);
    assertContains(pwd.toLowerCase(), output.toLowerCase());
  }

  public void testKillingParentKillsChildren() throws Exception {
    File destFile = getTempFile("tkpkc-file");
    File child1File = new File(destFile.getAbsolutePath() + "-child-1");
    File child2File = new File(destFile.getAbsolutePath() + "-child-2");
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain5.class.getName(), new String[] {
        destFile.getAbsolutePath(), "true" });

    process.start();

    StreamCollector stdout = new StreamCollector(process.STDOUT());
    stdout.start();
    StreamCollector stderr = new StreamCollector(process.STDERR());
    stderr.start();

    System.out.println("Waiting for parent to start");
    while (destFile.length() < 1) {
      ThreadUtil.reallySleep(1000);
    }
    System.out.println(" parent started");

    System.out.println("Waiting for child1 to start");
    while (child1File.length() < 1) {
      ThreadUtil.reallySleep(1000);
    }
    System.out.println(" child1 started");

    System.out.println("Waiting for child2 to start");
    while (child2File.length() < 1) {
      ThreadUtil.reallySleep(1000);
    }
    System.out.println(" child2 started");

    System.out.println(stdout.toString());
    System.out.println(stderr.toString());

    System.out.println("Killing parent");
    process.destroy();

    // wait for child process heartbeat to time out and kill themselves
    System.out.println("Waiting for children to be killed");
    Thread.sleep(HeartBeatServer.PULSE_INTERVAL * 2);

    long origSize = destFile.length();
    Thread.sleep(5000);
    long newSize = destFile.length();

    System.err.println("Parent after kill: new=" + newSize + "  old=" + origSize);
    assertEquals(origSize, newSize); // Make sure the parent is dead

    long child1OrigSize = child1File.length();
    long child2OrigSize = child2File.length();
    Thread.sleep(5000);
    long child1NewSize = child1File.length();
    long child2NewSize = child2File.length();
    System.err.println("Child 1 after kill: new=" + child1NewSize + "  old=" + child1OrigSize);
    System.err.println("Child 2 after kill: new=" + child2NewSize + "  old=" + child2OrigSize);

    assertEquals(child1NewSize, child1OrigSize); // Make sure child 1 is dead
    assertEquals(child2NewSize, child2OrigSize); // Make sure child 2 is dead
  }

  private static void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }
}
