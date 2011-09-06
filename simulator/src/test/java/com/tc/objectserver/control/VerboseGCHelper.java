/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.control;

import com.tc.util.runtime.Vm;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VerboseGCHelper {

  private static final String          XX_PRINT_GC_DETAILS         = "-XX:+PrintGCDetails";
  private static final String          XX_PRINT_GC_TIME_STAMPS     = "-XX:+PrintGCTimeStamps";
  private static final String          XLOGGC                      = "-Xloggc:";

  private static final String          XX_PRINT_GC_DETAILS_JROCKIT = "-Xverbose:gcpause,gcreport";
  private static final String          XLOGGC_JROCKIT              = "-Xverboselog:";

  private final static VerboseGCHelper instance                    = new VerboseGCHelper();
  private File                         tempDir;

  public static final VerboseGCHelper getInstance() {
    return instance;
  }

  private VerboseGCHelper() {
    //
  }

  public void setupTempDir(File tempDir) {
    this.tempDir = tempDir;
  }

  public void setupVerboseGcLogging(List<String> jvmArgs, String serverName, String mainClassName) {
    if (tempDir == null) {
      System.out.println("++++==================================================================================++++");
      System.out
          .println("Verbose GC logging will not be setup for this run (tempDir is null). Please set up tempDir to start using verbose gc for this run - serverName="
                   + serverName + ", mainClassName=" + mainClassName);
      System.out.println("++++==================================================================================++++");
      return;
    }
    File verboseGcDir = new File(tempDir, "server-verboseGC");
    verboseGcDir.mkdirs();
    if (!verboseGcDir.exists()) { throw new AssertionError("Failed to create verbose gc logs dir"); }
    File verboseGcOutputFile = new File(verboseGcDir, "verboseGC-" + (serverName == null ? "" : "(" + serverName + ")")
                                                      + mainClassName + "-started-" + getTimestamp() + ".log");
    if (verboseGcOutputFile.exists()) { throw new AssertionError("Verbose gc file: "
                                                                 + verboseGcOutputFile.getAbsolutePath()
                                                                 + " already exists"); }

    // remove existing verbose gc args
    for (Iterator<String> iter = jvmArgs.iterator(); iter.hasNext();) {
      String arg = iter.next();
      if (arg != null) {
        if (arg.startsWith(XLOGGC) || arg.startsWith(XX_PRINT_GC_TIME_STAMPS) || arg.startsWith(XX_PRINT_GC_DETAILS)
            || arg.startsWith(XLOGGC_JROCKIT) || arg.startsWith(XX_PRINT_GC_DETAILS_JROCKIT)) {
          System.out.println("XXX: Removing previous verbose gc arg: " + arg);
          iter.remove();
        }
      }
    }

    if (Vm.isJRockit()) {
      jvmArgs.add(XX_PRINT_GC_DETAILS_JROCKIT);
      jvmArgs.add(XLOGGC_JROCKIT + verboseGcOutputFile.getAbsolutePath());
    } else {
      jvmArgs.add(XLOGGC + verboseGcOutputFile.getAbsolutePath());
      jvmArgs.add(XX_PRINT_GC_TIME_STAMPS);
      jvmArgs.add(XX_PRINT_GC_DETAILS);
    }
  }

  private static String getTimestamp() {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss.S");
    return fmt.format(new Date());
  }

}
