/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.process;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Exec {

  public static Result execute(String cmd[]) throws Exception {
    return execute(cmd, null, null, null);
  }

  public static Result execute(String cmd[], String outputLog) throws Exception {
    return execute(cmd, outputLog, null, null);
  }

  public static Result execute(String cmd[], String outputLog, byte[] input) throws Exception {
    return execute(cmd, outputLog, input, null);
  }

  public static Result execute(String cmd[], String outputLog, byte[] input, File workingDir) throws Exception {
    Process process = Runtime.getRuntime().exec(cmd, null, workingDir);

    Thread inputThread = new InputPumper(input == null ? new byte[] {} : input, process.getOutputStream());

    StreamCollector stderr = null;
    StreamCollector stdout = null;

    FileOutputStream fileOutput = null;
    StreamAppender outputLogger = null;

    String errString = null;
    String outString = null;

    try {
      if (outputLog != null) {
        errString = "stderr output redirected to file " + outputLog;
        outString = "stdout output redirected to file " + outputLog;
        fileOutput = new FileOutputStream(outputLog);
        outputLogger = new StreamAppender(fileOutput);
        outputLogger.writeInput(process.getErrorStream(), process.getInputStream());
      } else {
        stderr = new StreamCollector(process.getErrorStream());
        stdout = new StreamCollector(process.getInputStream());
        stderr.start();
        stdout.start();
      }

      inputThread.start();

      final int exitCode = process.waitFor();

      if (inputThread != null) inputThread.join();

      if (outputLogger != null) {
        outputLogger.finish();
      }

      if (stderr != null) {
        stderr.join();
        errString = stderr.toString();
      }

      if (stdout != null) {
        stdout.join();
        outString = stdout.toString();
      }

      return new Result(cmd, outString, errString, exitCode);
    } finally {
      closeQuietly(fileOutput);
    }
  }

  private static void closeQuietly(OutputStream output) {
    if (output != null) {
      try {
        output.close();
      } catch (IOException ioe) {
        // quiet
      }
    }
  }

  private static class InputPumper extends Thread {
    private final InputStream  data;
    private final OutputStream output;

    InputPumper(byte[] input, OutputStream output) {
      this.output = output;
      this.data = new ByteArrayInputStream(input);
    }

    public void run() {
      try {
        IOUtils.copy(data, output);
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        closeQuietly(output);
      }
    }
  }

  public static class Result {
    private final String   stderr;
    private final String   stdout;
    private final int      exitCode;
    private final String[] cmd;

    private Result(String[] cmd, String stdout, String stderr, int exitCode) {
      this.cmd = cmd;
      this.stdout = stdout;
      this.stderr = stderr;
      this.exitCode = exitCode;
    }

    public String getStderr() {
      return stderr;
    }

    public String getStdout() {
      return stdout;
    }

    public int getExitCode() {
      return exitCode;
    }

    public String toString() {
      return "Command: " + Arrays.asList(cmd) + "\n" + "exit code: " + exitCode + "\n" + "stdout: " + stdout + "\n"
          + "stderr: " + stderr + "\n";
    }

  }

}
