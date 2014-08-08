package com.terracotta.management.test;

import com.tc.config.test.schema.ConfigHelper;
import com.tc.test.config.model.TestConfig;

/**
 * OperatorEventsTest
 */
public class OperatorEventsTest extends AbstractTsaAgentTestBase {
  private static final int GROUP_COUNT = 1; // cannot have Active-Active with Open Source
  private static final int MEMBER_COUNT = 1;

  public OperatorEventsTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.setNumOfGroups(GROUP_COUNT);
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
        getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
            "/tc-management-api/agents/operatorEvents");
      }

      for (int serverIndex = 0; serverIndex < MEMBER_COUNT; serverIndex++) {
        getTsaJSONArrayContent(ConfigHelper.HOST, getGroupData(0).getManagementPort(serverIndex),
                "/tc-management-api/agents/operatorEvents?sinceWhen=1377125095225&filterOutRead=true");
      }

    }
  }
}
