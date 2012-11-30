/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.express.tests.util.KeyValueGenerator;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class AbstractToolkitRejoinTest extends AbstractToolkitTestBase {

  private static final int  DEFAULT_CLIENT_RECONNECT_WINDOW_SECONDS = 1;
  private static final long DEFAULT_OOO_TIMEOUT_MILLIS              = TimeUnit.SECONDS.toMillis(20L);
  private static final int  DEFAULT_ELECTION_TIME                   = 5;
  private final boolean     isPermanentStore                        = true;
  private final long        oooTimeoutMillis                        = DEFAULT_OOO_TIMEOUT_MILLIS;
  private final int         clientReconnectWindowSecs               = DEFAULT_CLIENT_RECONNECT_WINDOW_SECONDS;
  private final int         electionTimeSec                         = DEFAULT_ELECTION_TIME;
  protected boolean         startPassive                            = true;

  public AbstractToolkitRejoinTest(TestConfig testConfig, Class<? extends ClientBase>... c) {
    super(testConfig, c);
    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getGroupConfig().setElectionTime(this.electionTimeSec);
    if (isPermanentStore) {
      testConfig.getL2Config().setRestartable(true);
    } else {
      testConfig.getL2Config().setRestartable(false);
    }
    testConfig.getL2Config().setClientReconnectWindow(this.clientReconnectWindowSecs);

    testConfig.addTcProperty("l2.l1reconnect.enabled", "true");
    testConfig.addTcProperty("l2.l1reconnect.timeout.millis", Long.toString(this.oooTimeoutMillis));
  }

  public static abstract class AbstractToolkitRejoinTestClient extends ClientBase {
    private ToolkitLogger              logger;
    protected final List<ClusterEvent> receivedEvents = new CopyOnWriteArrayList<ClusterEvent>();
    private TKStatefulClusterListener  statefulListener;
    protected KeyValueGenerator        keyValueGenerator;

    public AbstractToolkitRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTest() throws Throwable {
      doRejoinTest(getTestControlMbean());
    }

    protected ToolkitInternal createRejoinToolkit() throws Exception {
      debug("Creating rejoin toolkit");
      Properties properties = new Properties();
      properties.put("rejoin", "true");
      ToolkitInternal tk = (ToolkitInternal) ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(),
                                                                          properties);
      logger = tk.getLogger("com.tc.AppLogger");
      statefulListener = new TKStatefulClusterListener(tk);
      return tk;
    }

    protected void startRejoinAndWaitUntilPassiveStandBy(TestHandlerMBean testHandlerMBean, ToolkitInternal tk)
        throws Exception {
      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      doDebug("Starting last crashed server...");
      testHandlerMBean.restartLastCrashedServer(0);
      waitUntilPassiveStandBy(testHandlerMBean);
    }

    protected void waitUntilPassiveStandBy(TestHandlerMBean testHandlerMBean) throws Exception {
      doDebug("Waiting for passive to come up in cluster");
      testHandlerMBean.waitUntilPassiveStandBy(0);
    }

    protected void startRejoinAndWaitUntilCompleted(TestHandlerMBean testHandlerMBean, ToolkitInternal tk)
        throws Exception {
      tk.waitUntilAllTransactionsComplete();
      doDebug("Crashing first active...");
      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);
      doDebug("Passive must have taken over as ACTIVE");
      waitUntilRejoinCompleted();
    }

    protected void waitUntilRejoinCompleted() {
      statefulListener.waitUntilRejoin();
      doDebug("Rejoin happened successfully");
    }

    protected void doSleep(int sec) {
      for (int i = 0; i < sec; i++) {
        // doDebug("Sleeping for 1 sec (" + i + "/" + sec + ")");
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          //
        }
      }
    }

    protected void doDebug(String string) {
      debug(string);
      if (logger != null) {
        logger.info("___XXX___: " + string);
      }
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      throw new RuntimeException("This method should not be used for rejoin tests");
    }

    protected String getPermGenUsage() {
      String message = "";
      List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
      for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
        String name = memoryPoolMXBean.getName();
        if (!name.contains("Perm Gen")) {
          continue;
        }
        MemoryUsage usage = memoryPoolMXBean.getUsage();
        message += " (" + name + " : " + toMegaBytes(usage.getUsed()) + "M / " + toMegaBytes(usage.getMax()) + "M)";
      }
      return message;
    }

    private long toMegaBytes(long bytes) {
      return (bytes / 1024) / 1024;
    }

    protected abstract void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable;
  }

}
