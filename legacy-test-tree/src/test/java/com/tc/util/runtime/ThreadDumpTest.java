/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.process.StreamCollector;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;
import com.tc.util.runtime.ThreadDump.PID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ThreadDumpTest extends TCTestCase {

   public ThreadDumpTest() {
     disableAllUntil("2011-11-01");
   }

  // XXX: This test is known to fail under jrockit on the monkey. When we decide to deal with JRockit, we'll have to get
  // this thing working too. One alternative: If there is a magic jrockit specific way to get thread dumps, feel to try
  // it instead of kill -3 or CTRL-Break

  public void testDump() throws IOException, InterruptedException {
    LinkedJavaProcess process = new LinkedJavaProcess(ThreadDump.class.getName());

    List args = new ArrayList<String>();
    args.add("-D" + TestConfigObject.TC_BASE_DIR + "=" + System.getProperty(TestConfigObject.TC_BASE_DIR));
    args.add("-D" + TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME + "="
             + System.getProperty(TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME));
    if (Vm.isIBM()) {
      args.add("-Xdump:java:file=-");
    }
    process.addAllJvmArgs(args);

    System.err.println("JAVA ARGS: " + args);

    process.start();

    StreamCollector err = new StreamCollector(process.STDERR());
    StreamCollector out = new StreamCollector(process.STDOUT());

    err.start();
    out.start();

    process.waitFor();

    err.join();
    out.join();

    String stderr = err.toString();
    String stdout = out.toString();

    System.out.println("**** STDOUT BEGIN ****\n" + stdout + "\n**** STDOUT END ****");
    System.out.println("**** STDERR BEGIN ****\n" + stderr + "\n**** STDERR END ****");

    assertTrue(stderr.toLowerCase().indexOf("full thread dump") >= 0
               || stdout.toLowerCase().indexOf("full thread dump") >= 0);
  }

  public void testPidMechanismsAreSame() {
    int jniPID = GetPid.getInstance().getPid();
    int fallback = ThreadDump.getPIDUsingFallback().getPid();
    assertEquals(jniPID, fallback);
  }

  public void testFindAllJavaPIDs() {
    Set<PID> allPIDs = ThreadDump.findAllJavaPIDs();
    System.err.println("ALL: " + allPIDs);

    PID pid = ThreadDump.getPID();
    System.err.println("PID: " + pid);

    assertTrue(allPIDs.contains(pid));
  }
}
