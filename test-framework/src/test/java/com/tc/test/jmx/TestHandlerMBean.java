package com.tc.test.jmx;

import com.tc.test.setup.GroupsData;

public interface TestHandlerMBean {

  public void crashActiveServer(int groupIndex) throws Exception;

  public void restartCrashedServer(int groupIndex, int serverIndex) throws Exception;
  
  public void reastartLastCrashedServer(final int groupIndex) throws Exception;

  public void dumpClusterState() throws Exception;

  public void crashAllPassiveServers(int groupIndex) throws Exception;

  public void crashServer(int groupIndex, int serverIndex) throws Exception;

  public boolean isActivePresent(int groupIndex) throws Exception;

  public boolean isPassiveStandBy(int groupIndex) throws Exception;

  public void waitUntilPassiveStandBy(int groupIndex) throws Exception;

  public GroupsData[] getGroupsData();

  public void crashActiveAndWaitForPassiveToTakeOver(int groupIndex) throws Exception;
  
  public String getTerracottaUrl();
  
  public int getParticipantCount();

}