/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

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

    public AbstractToolkitRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      doRejoinTest(getTestControlMbean());
    }

    protected abstract void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable;
  }

}
