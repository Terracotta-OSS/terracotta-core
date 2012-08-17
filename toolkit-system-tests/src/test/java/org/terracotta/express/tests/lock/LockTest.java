/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.lock;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LockTest extends AbstractToolkitTestBase {
  public static final String clientBucket = "LockClientBucket";

  public LockTest(TestConfig testConfig) {
    super(testConfig, LockClient1.class, LockClient2.class);
    testConfig.getClientConfig().setParallelClients(true);
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File clientOutput) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, clientOutput);

    FileReader fr = null;
    try {
      fr = new FileReader(clientOutput);
      BufferedReader reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        if (st.contains("org.terracotta.express.tests.lock.LockClient1-"
                        + "org.terracotta.express.tests.lock.LockClient2")) return;
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
