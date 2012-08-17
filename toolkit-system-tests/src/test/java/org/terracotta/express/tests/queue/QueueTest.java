/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.queue;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class QueueTest extends AbstractToolkitTestBase {

  public QueueTest(TestConfig testConfig) {
    super(testConfig, QueueClient1.class, QueueClient2.class);
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, clientOutput);
    if ("org.terracotta.express.tests.queue.QueueClient1".equals(clientName)) {
      FileReader fr = null;
      try {
        fr = new FileReader(clientOutput);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("There's a hole in the bucket")) return;
        }
        throw new AssertionError("Client " + clientName + " did not pass");
      } catch (Exception e) {
        throw new AssertionError(e);
      } finally {
        try {
          fr.close();
        } catch (Exception e) {
          //
        }
      }
    }
  }
}
