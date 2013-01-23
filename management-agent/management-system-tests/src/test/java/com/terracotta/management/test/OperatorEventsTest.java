package com.terracotta.management.test;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

/**
 * OperatorEventsTest
 */
public class OperatorEventsTest extends AbstractTsaAgentTestBase {

  private static final int MEMBER_COUNT = 2;

  public OperatorEventsTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getGroupConfig().setMemberCount(MEMBER_COUNT);

    testConfig.getClientConfig().setClientClasses(new Class[] {OperatorEventsTestClient.class});
  }

  public static class OperatorEventsTestClient extends AbstractTsaClient {

    public OperatorEventsTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTsaTest() throws Throwable {
      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getTsaGroupPort(serverIndex),
            "/tc-management-api/agents/operatorEvents");
      }
    }
  }
}
