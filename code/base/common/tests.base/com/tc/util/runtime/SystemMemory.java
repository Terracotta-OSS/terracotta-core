/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.runtime;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SystemMemory {

  private static final long MB = 1024L * 1024L;

  private static final long GB = MB * 1024L;

  public static long getTotalSystemMemory() {
    if (!Os.isSolaris()) { throw new UnsupportedOperationException("No support exists for " + Os.getOsName()); }
    return getSolarisMemory();
  }

  private static long getSolarisMemory() {
    final Result result;

    try {
      result = Exec.execute(new String[] { "/usr/sbin/prtdiag" });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (result.getExitCode() != 0) { throw new RuntimeException("non-zero exit code: " + result); }

    Pattern pattern = Pattern.compile("Memory size: (\\d+)\\.(\\d+)(GB|MB)");
    Matcher matcher = pattern.matcher(findMemLine(result.getStdout()));

    if (!matcher.matches()) { throw new RuntimeException("cannot find expected output: " + result); }

    String memWhole = matcher.group(1);
    String memFraction = matcher.group(2);
    String memUnit = matcher.group(3);

    long rv = Long.parseLong(memWhole + memFraction);
    if (memUnit.equals("GB")) {
      rv *= GB;
    } else if (memUnit.equals("MB")) {
      rv *= MB;
    } else {
      throw new RuntimeException("unexpected unit: " + memUnit);

    }
    return rv / pow10(memFraction.length());
  }

  private static String findMemLine(String stdout) {
    int start = stdout.indexOf("\nMemory size: ");
    if (start < 0) { throw new RuntimeException("no start: " + stdout); }
    String rv = stdout.substring(start + 1);

    int end = rv.indexOf("\n");
    if (end < 0) { throw new RuntimeException("no end: " + rv); }
    rv = rv.substring(0, end);

    return rv.trim();
  }

  private static long pow10(int length) {
    if (length < 1) { throw new AssertionError("invalid length: " + length); }
    long rv = 1;
    for (int i = 0; i < length; i++) {
      rv *= 10;
    }
    return rv;
  }

}
