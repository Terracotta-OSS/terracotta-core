/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.tests.base;

import org.terracotta.test.util.LogUtil;

import com.tc.test.config.model.ClientConfig;
import com.tc.test.config.model.L2Config;
import com.tc.test.config.model.PauseConfig;
import com.tc.test.config.model.ServerPauseMode;
import com.tc.test.config.model.TestConfig;
import com.tc.test.setup.TestServerManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Only One Manager for Each TestCase or Test Config
 */
public final class PauseManager {

  private final TestConfig config;
  private TestServerManager   serverManager;
  private TestClientManager   clientManager;                        // not final because of Circular Dependency  among
                                                                    // ClientManager and PauseManager
  private final ScheduledExecutorService schedulerService = Executors.newScheduledThreadPool(5);
  private final Map<Integer, ScheduledFuture<?>> schedulersForClients;    // needed to keep futures for cancellation

  public PauseManager(TestConfig config) {
    this.config = config;
    this.schedulersForClients = new ConcurrentHashMap<Integer, ScheduledFuture<?>>();
  }
  
  void setClientManager(TestClientManager clientManager) {
    this.clientManager = clientManager;
  }
  
  void setTestServerManager(TestServerManager testServerManager) {
    this.serverManager = testServerManager;
  }

  /**
   * Called by TestHandlerMBean Implementation. Schedule Pauses based on pauseConfig for the active server of group
   * specified by groupIndex.
   */
  public void pauseActiveServer(final int groupIndex, final PauseConfig pauseConfig) {
    final int activeServerIndex = serverManager.getActiveServerIndex(groupIndex);
    PauseTask pauseTask = new PauseTask() {
      @Override
      public void executeTask() throws InterruptedException {
        serverManager.pauseServer(groupIndex, activeServerIndex, pauseConfig.getPauseTime());
      }
    };
    scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
  }

  /**
   * Called by TestHandlerMBean Implementation.Schedule Pauses based on pauseConfig for all passive servers of group
   * specified by groupIndex.
   */
  public void pausePassiveServers(final int groupIndex, final PauseConfig pauseConfig) {
    schedulePauseForAllPassive(groupIndex, pauseConfig);
  }

  /**
   * Will Pause the server identified by groupIndex and severIndex (if it is running)
   */
  public void pauseServer(final int groupIndex, final int serverIndex, final PauseConfig pauseConfig) {
    PauseTask pauseTask = new PauseTask() {
      @Override
      public void executeTask() throws InterruptedException {
        serverManager.pauseServer(groupIndex, serverIndex, pauseConfig.getPauseTime());
      }
    };
    scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
  }

  /**
   * Called by TestHandlerMBean Implementation. Will throw IllegalArgumentException if already PauseConfig exist for the
   * corresponding client Index. We cannot configure two pauseConfig for a single client process.
   */
  public void pauseClient(final int clientIndex, final PauseConfig pauseConfig) {
    ClientConfig clientConfig = config.getClientConfig();
    final PauseConfig startupPauseConfig = clientConfig.getClientPauseConfig(clientIndex);
    if (startupPauseConfig != null) {
      // may not be the best Exception Type Here
      throw new IllegalArgumentException("PauseConfig for Client Index : "
          + clientIndex
          + "is already defined in ClientConfig "); }
    pauseClientInternal(clientIndex, pauseConfig);
  }


  /**
   * AbstractTestBase call this method to initiate pauses for l2 as defined in l2config.
   */
  void startServerPauseTasks() {
    if (config.isPauseFeatureEnabled()) {
      LogUtil.info(PauseManager.class, "Pause Manager Started for Server");
      manageServerPauses();
    }
  }

  /**
   * TestClientManager calls this method whenever a new Client Process is created.
   */
  void startPauseConfig(final int clientIndex) {
    ClientConfig clientConfig = config.getClientConfig();
    final PauseConfig pauseConfig = clientConfig.getClientPauseConfig(clientIndex);
    if (pauseConfig != null) {
      pauseClientInternal(clientIndex, pauseConfig);
    }
  }

  /**
   * TestClientManager calls this method whenever a Client Process is terminated
   */
  void stopPauseConfig(final int clientIndex) {
    ScheduledFuture<?> future = schedulersForClients.get(clientIndex);
    if (future != null) {
      future.cancel(false);
    }
  }

  private void pauseClientInternal(final int clientIndex, final PauseConfig pauseConfig) {
    PauseTask pauseTask = new PauseTask() {
      @Override
      public void executeTask() throws InterruptedException {
        if (clientManager != null) {
          clientManager.pauseProcess(clientIndex, pauseConfig.getPauseTime());
        }
      }
    };
    ScheduledFuture<?> future = scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
    if (future != null) {
      schedulersForClients.put(clientIndex, future);// keeping timer reference for cancelling if the client process
                                               // terminates
    }
  }

  /**
   * for any test case there could be only one pauseMode active
   */
  private void manageServerPauses() {
    L2Config l2Config = config.getL2Config();
    final PauseConfig pauseConfig = l2Config.getPauseConfig();
    ServerPauseMode serverPauseMode = l2Config.getPauseMode();
    if (pauseConfig != null && serverPauseMode != null) {
    final int count = serverManager.getNumberOfGroups();
    switch (serverPauseMode) {
      case ANY_ACTIVE:
        pauseAnyActive(pauseConfig, count);
        break;
      case ALL_ACTIVE:
        pauseAllActive(pauseConfig, count);
        break;
        case ANY_GROUP_PASSIVE:
          pauseAnyGroupPassives(pauseConfig, count);
        break;
        case ALL_GROUP_PASSIVE:
          pauseAllGroupsPassive(pauseConfig, count);
        break;
      default:
        break;
    }
    }

  }

  private void pauseAnyGroupPassives(final PauseConfig pauseConfig, final int count) {
    PauseTask pauseTask = new PauseTask() {
      @Override
      public void executeTask() {
        int randomGroupIndex = getRandom(0, count - 1);
        schedulePauseForAllPassive(randomGroupIndex, pauseConfig);
      }
    };
    scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
  }

  private void pauseAllGroupsPassive(final PauseConfig pauseConfig, final int count) {
    for (int i = 0; i < count; i++) {
      schedulePauseForAllPassive(i, pauseConfig);
    }
  }

  private void schedulePauseForAllPassive(final int groupIndex, final PauseConfig pauseConfig) {
    
    final int activeServerIndex = serverManager.getActiveServerIndex(groupIndex);
    
    for (int j = 0; j < serverManager.getGroupData(groupIndex).getServerCount(); j++) {
      if (activeServerIndex == j) {
        continue;
      }
      final int serverIndex = j;
      PauseTask pauseTask = new PauseTask() {
        @Override
        public void executeTask() throws InterruptedException {
          serverManager.pauseServer(groupIndex, serverIndex, pauseConfig.getPauseTime());
        }
      };
      scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
    }
  }

  private void pauseAllActive(final PauseConfig pauseConfig, final int count) {

    for (int i = 0; i < count; i++) {
      final int groupIndex = i; // never wanted to do this
      final int activeServerIndex = serverManager.getActiveServerIndex(groupIndex);
      PauseTask pauseTask = new PauseTask() {
        @Override
        public void executeTask() throws InterruptedException {
          serverManager.pauseServer(groupIndex, activeServerIndex, pauseConfig.getPauseTime());
        }
      };
      scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
    }
  }

  private void pauseAnyActive(final PauseConfig pauseConfig, final int count) {
    PauseTask pauseTask = new PauseTask() {
      @Override
      public void executeTask() throws InterruptedException {
        int randomGroupIndex = getRandom(0, count - 1);
        final int activeServerIndex = serverManager.getActiveServerIndex(randomGroupIndex);
        serverManager.pauseServer(randomGroupIndex, activeServerIndex, pauseConfig.getPauseTime());
      }
    };
    scheduleTask(pauseConfig.getInitialDelay(), pauseConfig.getPauseInterval(), pauseTask);
  }

  private static int getRandom(int rangeStart, int rangeEnd) {
      if(rangeStart >= rangeEnd) {
        throw new IllegalArgumentException("RangeStart : "+ rangeStart+" should be less than RangeEnd :" + rangeEnd);
      }
    int random = (int) Math.floor(Math.random() * (rangeEnd - rangeStart));
    random = random + rangeStart;
    if (random > rangeEnd) { return rangeEnd; }
    return random;
  }


  /**
   * Every pause functionality will call this method irrespective of whether it is periodic or not
   */
  private ScheduledFuture<?> scheduleTask(final long initialDelay, final long timePeriod,
                                   final PauseTask task) {
    if (!config.isPauseFeatureEnabled()) { return null; }
    ScheduledFuture<?> future = schedulerService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        try {
          task.executeTask();
        } catch (Exception e) {
          e.printStackTrace();
          LogUtil.info(PauseManager.class, "Error while pausing");
        }
      }
    }, initialDelay, timePeriod, TimeUnit.MILLISECONDS);
    return future;
  }

  interface PauseTask {
    void executeTask() throws InterruptedException, IOException;
  }

  public void pauseClient(int clientIndex) throws InterruptedException {
    if (!isPauseEnabled()) { return; }
    clientManager.pauseClient(clientIndex);

  }

  public void unpauseClient(int clientIndex) throws InterruptedException {
    if (!isPauseEnabled()) { return; }
    clientManager.unpauseClient(clientIndex);
  }

  public void pauseServer(int groupIndex, int serverIndex) throws InterruptedException {
    if (!isPauseEnabled()) { return; }
    serverManager.pauseServer(groupIndex, serverIndex);
  }

  public void unpauseServer(int groupIndex, int serverIndex) throws InterruptedException {
    if (!isPauseEnabled()) { return; }
    serverManager.unpauseServer(groupIndex, serverIndex);
  }

  private boolean isPauseEnabled() {
    boolean result = config.isPauseFeatureEnabled();
    if (!result) {
      LogUtil.debug(PauseManager.class, " Pause Feature is not Enabled");
    }
    return result;
  }

}
