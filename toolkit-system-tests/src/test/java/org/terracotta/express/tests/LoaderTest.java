/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.process.Exec;
import com.tc.test.config.model.TestConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LoaderTest extends AbstractToolkitTestBase {

  public LoaderTest(TestConfig testConfig) {
    super(testConfig);
  }

  @Override
  protected void startClients() throws Throwable {
    runClient(LoaderClient.class);
  }

  @Override
  protected void runClient(Class client) throws Throwable {
    String clientName = client.getSimpleName();
    String test = TestBaseUtil.jarFor(client);
    String toolkit = TestBaseUtil.jarFor(ToolkitFactory.class);

    List<String> cmd = new ArrayList<String>();
    cmd.add(Exec.getJavaExecutable());

    cmd.add("-Xms128m");
    cmd.add("-Xmx128m");
    cmd.add("-cp");
    cmd.add(makeClasspath(test, toolkit));
    cmd.add(client.getName());
    cmd.add(getTerracottaURL());

    System.err.println("Starting client with cmd: " + cmd);

    File workDir = new File(tempDir + "/" + clientName);
    workDir.mkdirs();
    File output = new File(workDir, clientName + ".log");
    System.out.println("client output file: " + output.getAbsolutePath());
    System.out.println("working directory: " + workDir.getAbsolutePath());
    Exec.Result result = Exec.execute(cmd.toArray(new String[cmd.size()]), output.getAbsolutePath(), null, workDir);

    evaluateClientOutput(client.getName(), result.getExitCode(), output);
  }

}
