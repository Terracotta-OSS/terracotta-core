package com.tc.test.jmx;

import com.tc.test.setup.GroupsData;

import java.io.File;
import java.io.Serializable;
import java.util.List;

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
  public void restartLastCrashedServer(final int groupIndex) throws Exception;

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
   * Crashes a specific passive from the given group
   * 
   * @param groupIndex
   * @param serverIndex
   * @throws Exception
   */
  public void crashPassive(int groupIndex, int serverIndex) throws Exception;

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
   * Check if the given server is passive uninitialized.
   *
   * @param groupIndex group number
   * @param serverIndex server number
   * @return true if the server is passive uninitialized
   */
  public boolean isPassiveUninitialized(int groupIndex, int serverIndex);

  /**
   * Waits until a passive standby comes up in the mirror group
   * 
   * @param groupIndex
   * @throws Exception
   */
  public void waitUntilPassiveStandBy(int groupIndex) throws Exception;

  /**
   * Waits until every passive in group goes into standby
   */
  public void waitUntilEveryPassiveStandBy(int groupIndex) throws Exception;

  /**
   * Wait until there is an active in the group.
   *
   * @param groupIndex index of the group
   * @throws Exception
   */
  public void waitUntilActive(int groupIndex) throws Exception;

  /**
   * Returns all the groups data for each mirror group ` ``````````````````````
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
   * "server1Host:server1TsaPort,server2Host:server2TsaPort,server3Host:server3TsaPort,server4Host:server4TsaPort"
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

  /**
   * Starts the given server.
   * 
   * @param groupIndex group that the server belongs to
   * @param serverIndex the server in the group to start
   */
  public void startServer(int groupIndex, int serverIndex) throws Exception;

  /**
   * @return true if the test is configured to run in standAlone mode, false otherwise
   */
  public boolean isStandAloneTest();

  /**
   * Prints the stackTrace in testFramework logs if Client exits abnormally
   * 
   * @param t exception that the client exited with.
   */
  public void clientExitedWithException(Throwable t);

  /**
   * Check if the server is running.
   * 
   * @param groupIndex group for the server
   * @param serverIndex server index
   * @return true if the server is running.
   */
  public boolean isServerRunning(int groupIndex, int serverIndex);

  /**
   * Wait for a server to exit, returning the exit code.
   * 
   * @param groupIndex group for the server
   * @param serverIndex server index
   * @return exit code
   */
  public int waitForServerExit(int groupIndex, int serverIndex) throws Exception;

  /**
   * Disconnects the client from this group
   * 
   * @param groupIndex group for the server
   */
  public void stopTsaProxy(int groupIndex) throws Exception;

  public void startTsaProxy(int groupIndex) throws Exception;

  /**
   * Closes the connections between the clients and server for the given group
   *
   * @param groupIndex group to close connections on
   * @throws Exception
   */
  public void closeTsaProxyConnections(int groupIndex) throws Exception;

  /**
   * Execute custom command
   */
  public Serializable executeCustomCommand(String cmd, Serializable[] params);

  /**
   * returns the tc url with proxyL1Ports<br>
   * e.g if there are two mirror groups having two servers in each this will return
   * "server1Host:server1TsaPort,server2Host:server2TsaPort,server3Host:server3TsaPort,server4Host:server4TsaPort"
   */
  String getTsaProxyTerracottaUrl();

  /**
   * returns the tc config file after replacing the tsa ports with proxy ports<br>
   * e.g if there are two mirror groups having two servers in each this will return
   * "server1Host:server1TsaPort,server2Host:server2TsaPort,server3Host:server3TsaPort,server4Host:server4TsaPort"
   * 
   * @throws Exception
   */
  String getTsaProxyTcConfig() throws Exception;

  /**
   * returns the absolute path of tc-config-proxy file
   */

  String getTsaProxyTcConfigFilePath();

  /**
   * Runs the given client class.
   *
   * @param client class of the client to run.
   * @param clientName
   * @param extraMainClassArgs
   */
  void runClient(Class<? extends Runnable> client, final String clientName, final List<String> extraMainClassArgs) throws Throwable;

  /**
   * starts server crasher, can be used in the tests that wants to start the crasher after doing some initialization
   * tasks
   */
  void startCrasher();

  int getActiveServerIndex(int groupIndex);

  void pauseClient(int clientIndex) throws Exception;

  void unpauseClient(int clientIndex) throws Exception;

  void pauseServer(int groupIndex, int serverIndex) throws Exception;

  void unpauseServer(int groupIndex, int serverIndex) throws Exception;

  File getTempDir();

}
