package com.tc.test.jmx;

import com.tc.test.setup.GroupsData;

public interface TestHandlerMBean {

  /**
   * Crashes the active server in the specified mirror group
   * 
   * @param groupIndex : the index of the mirror group for which the active needs to be crashed
   * @throws Exception
   */
  public void crashActiveServer(int groupIndex) throws Exception;

  /**
   * Restarts the server which crashed in a particular mirror group <br>
   * Note that responsibility of maintaining the serverIndex for the server which crashed last for the mirror group
   * comes to the user
   * 
   * @param groupIndex the group index in which the server is to be crashed
   * @param serverIndex the last crashed server index in the mirror group
   * @throws Exception
   */
  public void restartCrashedServer(int groupIndex, int serverIndex) throws Exception;

  /**
   * Restarts the last crashed server in the specified mirror group
   * 
   * @param groupIndex the index of the mirror group in which the server is to be restarted
   * @throws Exception
   */
  public void reastartLastCrashedServer(final int groupIndex) throws Exception;

  /**
   * Dumps the state for all servers and clients
   * 
   * @throws Exception
   */
  public void dumpClusterState() throws Exception;

  /**
   * Crashes all the passive servers in the specified mirror group
   * 
   * @param groupIndex
   * @throws Exception
   */
  public void crashAllPassiveServers(int groupIndex) throws Exception;

  /**
   * Finds out whether an active is present in a particular mirror group
   * 
   * @param groupIndex the group index
   * @return true if an active is present in this mirror group
   * @throws Exception
   */
  public boolean isActivePresent(int groupIndex) throws Exception;

  /**
   * Finds out whether a passive stand by is present in a particular group
   * 
   * @param groupIndex
   * @return true is passive stand by is present in this mirror group
   * @throws Exception
   */
  public boolean isPassiveStandBy(int groupIndex) throws Exception;

  /**
   * Waits until a passive standby comes up in the mirror group
   * 
   * @param groupIndex
   * @throws Exception
   */
  public void waitUntilPassiveStandBy(int groupIndex) throws Exception;

  /**
   * Returns all the groups data for each mirror group
   * 
   * @return
   */
  public GroupsData[] getGroupsData();

  /**
   * Crashes the active and waits till passive becomes active in a specified mirror group
   * 
   * @param groupIndex
   * @throws Exception
   */
  public void crashActiveAndWaitForPassiveToTakeOver(int groupIndex) throws Exception;

  /**
   * returns the tc url <br>
   * e.g if there are two mirror groups having two servers in each this will return
   * "server1Host:server1DsoPort,server2Host:server2DsoPort,server3Host:serverOne3Port,server4Host:server4DsoPort"
   * 
   * @return
   */
  public String getTerracottaUrl();

  /**
   * Returns the number of clients started for the test
   * 
   * @return
   */
  public int getParticipantCount();

}