/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.locks.ReadWriteLock;

public class ClusterDumperSystemTest extends AbstractToolkitTestBase {

  public ClusterDumperSystemTest(TestConfig testConfig) {
    super(testConfig, Client.class);
  }

  private File[] getClientLogFiles(File output) {
    File[] files = output.getParentFile().listFiles();

    for (File file : files) {
      if (file.getName().startsWith("logs")) { return file.listFiles(); }
    }
    return null;
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, output);
    // Check for LOCK Info in Dump in terracotta-client.log files

    File[] clientLogFiles = getClientLogFiles(output);
    for (File clientLog : clientLogFiles) {
      FileReader fr = null;
      try {
        System.out.println("Examining Client Log File" + clientLog.getAbsolutePath());
        fr = new FileReader(clientLog);
        BufferedReader reader = new BufferedReader(fr);
        String st = "";
        while ((st = reader.readLine()) != null) {
          if (st.contains("LOCKED : [StringLockID(")) return;
        }
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
    throw new AssertionError("Client " + clientName + " could not find Lock Info in thread dump");
  }

  public static class Client extends ClientBase {

    public Client(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ReadWriteLock readWriteLock = toolkit.getReadWriteLock("lock");
      readWriteLock.writeLock().lock();
      try {
        getTestControlMbean().dumpClusterState();
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

  }

}
