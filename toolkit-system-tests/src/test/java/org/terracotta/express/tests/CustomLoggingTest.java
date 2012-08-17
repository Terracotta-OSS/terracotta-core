/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.apache.commons.io.IOUtils;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Random;

public class CustomLoggingTest extends AbstractToolkitTestBase {
  private static final String PREFIX = "[THIS IS THE CUSTOM LOGGING]";
  private final String        uniqueTag;

  public CustomLoggingTest(TestConfig testConfig) {
    super(testConfig, CustomLoggingClient.class);
    this.uniqueTag = PREFIX + " " + getClass().getName() + "[" + new Random().nextLong() + "]";
  }

  @Override
  protected void preStart(File workDir) {
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(new File(workDir, ".tc.custom.log4j.properties"));
      PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
      pw.println("log4j.logger.com.terracottatech.dso=INFO, A1");
      pw.println("log4j.appender.A1=org.apache.log4j.ConsoleAppender");
      pw.println("log4j.appender.A1.layout=org.apache.log4j.PatternLayout");
      pw.println("log4j.appender.A1.layout.ConversionPattern=" + uniqueTag + " %-4r [%t] %-5p %c %x - %m%n");
      pw.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
    BufferedReader reader = new BufferedReader((new FileReader(clientOutput)));
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(uniqueTag)) {
          super.evaluateClientOutput(clientName, exitCode, clientOutput);
          return;
        }
      }
    } catch (IOException e) {
      throw new AssertionError(e.getMessage());
    }

    throw new AssertionError("Failed to find tag: " + uniqueTag);
  }
}
