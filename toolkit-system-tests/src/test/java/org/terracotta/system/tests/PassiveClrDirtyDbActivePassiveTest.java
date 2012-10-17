/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;

import com.tc.objectserver.persistence.util.SetDbClean;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;

import gnu.trove.TLongObjectHashMap;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

/*
 * DEV-2011. For the case, both active and passive go down. But active goes down for good. Passive restores data and
 * becomes active.
 */
public class PassiveClrDirtyDbActivePassiveTest extends AbstractExpressActivePassiveTest {

  public PassiveClrDirtyDbActivePassiveTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
    testConfig.getCrashConfig().setShouldCleanDbOnCrash(false);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> extraJars = new ArrayList<String>();
    extraJars.add(TestBaseUtil.jarFor(BoundedBuffer.class));
    extraJars.add(TestBaseUtil.jarFor(ParseException.class));
    extraJars.add(TestBaseUtil.jarFor(FileUtils.class));
    extraJars.add(TestBaseUtil.jarFor(TLongObjectHashMap.class));
    extraJars.add(TestBaseUtil.jarFor(StringUtils.class));
    return extraJars;
  }

  public static class Writer extends ClientBase {
    public Writer(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitMap<String, String> map = toolkit.getMap("testMap", null, null);
      for (int i = 0; i < 100; i++) {
        map.put("" + i, "" + i);
      }
    }
  }

  public static class Reader extends ClientBase {
    public Reader(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ToolkitMap<String, String> map = toolkit.getMap("testMap", null, null);
      for (int i = 0; i < 100; i++) {
        Assert.assertEquals("" + i, map.get("" + i));
      }
    }
  }

  public static class DbCleanerApp extends ClientBase {
    private final String opt;
    private final String databaseDir;

    public DbCleanerApp(String[] args) {
      super(args);
      this.opt = args[1];
      this.databaseDir = args[2];
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      SetDbClean cleaner = new SetDbClean(new File(this.databaseDir), opt);
      cleaner.setDbClean();
    }

  }

  @Override
  protected void startClients() throws Throwable {
    System.out.println("XXX Starting test client");
    runClient(Writer.class);

    // Make sure the passive is fully synced before crashing it.
    testServerManager.waitUntilPassiveStandBy(0);

    System.out.println("XXX Stop passive server[1]");
    testServerManager.crashAllPassive(0);

    // clean up passive dirty db
    System.out.println("XXX Clean passive db dirty bit");

    String passiveDataLocation = new File(tempDir, "testserver1" + File.separator + "data" + File.separator
                                                   + "objectdb").getAbsolutePath();
    getTestConfig().getClientConfig().getExtraClientJvmArgs().addAll(getTCPropertyJvmArgs());
    runClient(DbCleanerApp.class, DbCleanerApp.class.getSimpleName(), Arrays.asList("-c", passiveDataLocation));

    System.out.println("XXX Stop active server[0]");
    testServerManager.crashActiveServer(0);

    Thread.sleep(10 * 1000);

    System.out.println("XXX Start passive server[1] as active");
    testServerManager.restartCrashedServer(0, 1);

    runClient(Reader.class);
  }

  private List<String> getTCPropertyJvmArgs() {
    RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
    List<String> tcPropertyDefines = new ArrayList<String>();
    for (String jvmArg : mxbean.getInputArguments()) {
      if (jvmArg.startsWith("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX)) {
        tcPropertyDefines.add(jvmArg);
      }
    }
    return tcPropertyDefines;
  }
}
