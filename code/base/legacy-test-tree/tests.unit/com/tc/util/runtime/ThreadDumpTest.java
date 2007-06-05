/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.process.LinkedJavaProcess;
import com.tc.process.StreamCollector;
import com.tc.test.TCTestCase;
import com.tc.test.TestConfigObject;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThreadDumpTest extends TCTestCase {

  private static final String IBM_COREFILE_NAME = "ThreadDumpTest-javacore.txt";

  // XXX: This test is known to fail under jrockit on the monkey. When we decide to deal with JRockit, we'll have to get
  // this thing working too. One alternative: If there is a magic jrockit specific way to get thread dumps, feel to try
  // it instead of kill -3 or CTRL-Break

  public void testDump() throws IOException, InterruptedException {
    LinkedJavaProcess process = new LinkedJavaProcess(ThreadDump.class.getName());

    List args = new ArrayList();
    String libPath = System.getProperty("java.library.path", "");
    if (!"".equals(libPath)) {
      args.add("-Djava.library.path=" + libPath);
    }

    args.add("-D" + TestConfigObject.TC_BASE_DIR + "=" + System.getProperty(TestConfigObject.TC_BASE_DIR));
    args.add("-D" + TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME + "="
             + System.getProperty(TestConfigObject.PROPERTY_FILE_LIST_PROPERTY_NAME));
    if (Vm.isIBM()) {
      args.add("-Xdump:java:file=" + IBM_COREFILE_NAME);
    }
    process.setJavaArguments((String[]) args.toArray(new String[args.size()]));

    System.err.println("JAVA ARGS: " + args);

    if (Vm.isIBM()) {
      assertFalse("Java core file already exists", ibmCoreFileExists());
    }
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

    if (Vm.isIBM()) {
      assertTrue(ibmCoreFileExists());
    } else {
      assertTrue(stderr.toLowerCase().indexOf("full thread dump") >= 0
                 || stdout.toLowerCase().indexOf("full thread dump") >= 0);
    }
  }

  private final boolean ibmCoreFileExists() {
    File[] coreFiles = new File(System.getProperty("user.dir")).listFiles(new FilenameFilter() {
      public boolean accept(final File dir, final String name) {
        return name.equals(IBM_COREFILE_NAME);
      }
    });
    return coreFiles.length == 1;
  }

  protected void tearDown() throws Exception {
    if (Vm.isIBM()) {
      // Try to remove the generated core file
      try {
        final File coreFile = new File(IBM_COREFILE_NAME);
        if (coreFile.exists() && coreFile.isFile()) {
          coreFile.deleteOnExit();
        }
      } catch (Exception ignore) {
        // Whatever, just trying to be nice
      }
    }
    super.tearDown();
  }

}
