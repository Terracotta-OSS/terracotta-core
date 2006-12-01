/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.process;

import com.tc.test.TCTestCase;
import com.tc.util.runtime.Os;

import java.io.File;

/**
 * Unit test for {@link LinkedJavaProcess}.
 */
public class LinkedJavaProcessTest extends TCTestCase {

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

  public static String ignoreStandardWarnings(String input) {
    String delimiter = System.getProperty("line.separator", "\n");

    String[] output = input.split(delimiter);
    StringBuffer out = new StringBuffer();
    for (int i = 0; i < output.length; ++i) {
      if (output[i].startsWith("DATA: ")) {
        out.append(output[i].substring("DATA: ".length()) + delimiter);
      }
    }

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

    String[] lines = output.split("[\\r\\n]+");
    assertEquals(strip(lines[0]), "ljpt.foo=myprop");

    if (Os.isWindows()) {
      // sometimes the case of the drive letter (ie. "C:\.." vs "c:\..") will be different here
      lines[1] = strip(lines[1]);
      lines[1] = lines[1].substring(0, 1).toLowerCase() + lines[1].substring(1);
      pwd = pwd.substring(0, 1).toLowerCase() + pwd.substring(1);
    }
    assertEquals(pwd, strip(lines[1]));
  }

  private String strip(String s) {
    if (s != null && s.startsWith("DATA: ")) return s.substring("DATA: ".length());
    else return s;
  }

  public void testKillingParentKillsChildren() throws Exception {
    File destFile = getTempFile("tkpkc-file");
    File child1File = new File(destFile.getAbsolutePath() + "-child-1");
    File child2File = new File(destFile.getAbsolutePath() + "-child-2");
    LinkedJavaProcess process = new LinkedJavaProcess(LinkedJavaProcessTestMain5.class.getName(), new String[] {
        destFile.getAbsolutePath(), "true" });

    process.start();

    StreamCollector collect = new StreamCollector(process.STDOUT());
    collect.start();
    StreamCollector collect2 = new StreamCollector(process.STDERR());
    collect2.start();

    long origSize = destFile.length();
    Thread.sleep(30000);
    long newSize = destFile.length();

    System.err.println("Parent first: " + newSize + " vs. " + origSize);
    assertTrue(newSize > origSize); // Make sure it's all started + working

    long child1OrigSize = child1File.length();
    long child2OrigSize = child2File.length();
    Thread.sleep(30000);
    long child1NewSize = child1File.length();
    long child2NewSize = child2File.length();

    System.err.println("Child 1 first: " + child1NewSize + " vs. " + child1OrigSize);
    System.err.println("Child 2 first: " + child2NewSize + " vs. " + child2OrigSize);
    // Make sure the children are all started + working
    assertTrue(child1NewSize > child1OrigSize);
    assertTrue(child2NewSize > child2OrigSize);

    process.destroy();
    Thread.sleep(15000);

    origSize = destFile.length();
    Thread.sleep(5000);
    newSize = destFile.length();

    System.err.println("Parent after kill: " + newSize + " vs. " + origSize);
    assertEquals(origSize, newSize); // Make sure the parent is dead

    child1OrigSize = child1File.length();
    child2OrigSize = child2File.length();
    Thread.sleep(5000);
    child1NewSize = child1File.length();
    child2NewSize = child2File.length();
    System.err.println("Child 1 after kill: " + child1NewSize + " vs. " + child1OrigSize);
    System.err.println("Child 2 after kill: " + child2NewSize + " vs. " + child2OrigSize);

    assertEquals(child1OrigSize, child1NewSize); // Make sure child 1 is dead
    assertEquals(child2OrigSize, child2NewSize); // Make sure child 1 is dead
  }

}
