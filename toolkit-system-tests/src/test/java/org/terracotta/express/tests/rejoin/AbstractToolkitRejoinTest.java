/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.cluster.RejoinClusterEvent;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
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

  public AbstractToolkitRejoinTest(TestConfig testConfig, Class<? extends AbstractToolkitRejoinTestClient>... c) {
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
    private ToolkitLogger logger;
    protected final List<ClusterEvent> receivedEvents = new CopyOnWriteArrayList<ClusterEvent>();
    protected ClusterNode              beforeRejoinNode;

    public AbstractToolkitRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTest() throws Throwable {
      doRejoinTest(getTestControlMbean());
    }

    public ToolkitInternal createRejoinToolkit() throws Exception {
      debug("Creating first toolkit");
      Properties properties = new Properties();
      properties.put("rejoin", "true");
      ToolkitInternal tk = (ToolkitInternal) ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(),
                                                                          properties);
      beforeRejoinNode = tk.getClusterInfo().getCurrentNode();
      logger = tk.getLogger("com.tc.AppLogger");
      tk.getClusterInfo().addClusterListener(new ClusterListener() {

        @Override
        public void onClusterEvent(ClusterEvent event) {
          doDebug("Received cluster event: " + event);
          receivedEvents.add(event);
        }
      });
      return tk;
    }

    protected void startRejoinAndWaitUnilRejoinHappened(TestHandlerMBean testHandlerMBean, ToolkitInternal tk) throws Exception {
      tk.waitUntilAllTransactionsComplete();

      doDebug("Crashing first active...");
      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);
      doDebug("Passive must have taken over as ACTIVE");

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          doDebug("Processing received events (waiting till rejoin happens for node: " + beforeRejoinNode + ")");
          for (ClusterEvent e : receivedEvents) {
            if (e instanceof RejoinClusterEvent) {
              RejoinClusterEvent re = (RejoinClusterEvent) e;
              doDebug("Rejoin event - oldNode: " + re.getNodeBeforeRejoin() + ", newNode: " + re.getNodeAfterRejoin());
              if (re.getNodeBeforeRejoin().getId().equals(beforeRejoinNode.getId())) {
                doDebug("Rejoin received for expected node - " + beforeRejoinNode);
                return true;
              }
            }
          }
          return false;
        }
      });

      doDebug("Rejoin happened successfully");
    }

    protected void doSleep(int sec) throws InterruptedException {
      for (int i = 0; i < sec; i++) {
        doDebug("Sleeping for 1 sec (" + i + "/" + sec + ")");
        Thread.sleep(1000);
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

    protected abstract void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable;
  }

}
