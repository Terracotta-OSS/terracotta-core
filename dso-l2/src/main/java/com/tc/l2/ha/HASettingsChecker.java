/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;

/*
 * Checks if the TC-Properties and TC-Config settings for Health Checker and Reconnect are adequate to provide High
 * Availability. The checks are inspired from the experiences described in Field War Stories (FWS) in JIRA. The checks
 * are just broad guidelines--production deployments will still hand tune Health Check settings and may choose to ignore
 * these warnings.
 * @author dkumar
 */
public class HASettingsChecker {

  private final L2ConfigurationSetupManager tcConfig;
  private final TCProperties                tcProperties;
  private static final TCLogger             logger = CustomerLogging.getDSOGenericLogger();
  private final boolean                     isHighAvailabilityEnabled;
  private final int                         l1l2PingIdleTime;
  private final int                         l1l2SocketConnectCount;
  private final int                         l1l2PingInterval;
  private final int                         l1l2PingProbes;
  private final int                         l1l2SocketConnectTimeout;
  private final int                         l2l2PingIdleTime;
  private final int                         l2l2SocketConnectCount;
  private final int                         l2l2PingInterval;
  private final int                         l2l2PingProbes;
  private final int                         l2l2SocketConnectTimeout;
  private final long                        clientReconnectWindow;
  private final long                        electionTime;

  private final int                         l1l2HealthCheckFailureTolerance;
  private final int                         l2l2HealthCheckFailureTolerance;

  public HASettingsChecker(L2ConfigurationSetupManager config, TCProperties props) {
    this.tcConfig = config;
    this.tcProperties = props;
    this.l1l2PingIdleTime = tcProperties.getInt(TCPropertiesConsts.L1_HEALTHCHECK_L2_PING_IDLETIME);
    this.l1l2SocketConnectCount = tcProperties.getInt(TCPropertiesConsts.L1_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT);
    this.l1l2PingInterval = tcProperties.getInt(TCPropertiesConsts.L1_HEALTHCHECK_L2_PING_INTERVAL);
    this.l1l2PingProbes = tcProperties.getInt(TCPropertiesConsts.L1_HEALTHCHECK_L2_PING_PROBES);
    this.l1l2SocketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L1_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT);
    this.l2l2PingIdleTime = tcProperties.getInt(TCPropertiesConsts.L2_HEALTHCHECK_L2_PING_IDLETIME);
    this.l2l2SocketConnectCount = tcProperties.getInt(TCPropertiesConsts.L2_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT);
    this.l2l2PingInterval = tcProperties.getInt(TCPropertiesConsts.L2_HEALTHCHECK_L2_PING_INTERVAL);
    this.l2l2PingProbes = tcProperties.getInt(TCPropertiesConsts.L2_HEALTHCHECK_L2_PING_PROBES);
    this.l2l2SocketConnectTimeout = tcProperties.getInt(TCPropertiesConsts.L2_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT);
    this.l1l2HealthCheckFailureTolerance = interNodeHealthCheckTime(l1l2PingIdleTime, l1l2SocketConnectCount,
                                                                    l1l2PingInterval, l1l2PingProbes,
                                                                    l1l2SocketConnectTimeout);
    this.l2l2HealthCheckFailureTolerance = interNodeHealthCheckTime(l2l2PingIdleTime, l2l2SocketConnectCount,
                                                                    l2l2PingInterval, l2l2PingProbes,
                                                                    l2l2SocketConnectTimeout);
    this.clientReconnectWindow = config.dsoL2Config().getDso().getClientReconnectWindow();
    this.electionTime = config.getActiveServerGroupForThisL2().getHaHolder().getHa().getNetworkedActivePassive()
        .getElectionTime();
    this.isHighAvailabilityEnabled = checkIfHighAvailabilityIsEnabled(tcConfig, tcProperties);
  }

  /*
   * High level validation method for checking if the Health Check settings at L1 and L2 can provide robust fail-over.
   */
  public void validateHealthCheckSettingsForHighAvailability() {
    if (isHighAvailabilityEnabled) {
      printWarningIfL1L2FailureToleranceHigherThanL2L2FailureTolerance();
      printWarningIfL1DisconnectIsLowerThanL2DisconnectPlusElectionTime();
    }

  }

  /*
   * To guard against the scenario described in DEV-6345 and FWS-101. Checks if there is enough time for L1s to fail
   * over to a new ACTIVE L2 by taking into account L2 election time and client reconnect window. Prints a warning if
   * L1-L2HealthCheck > L2-L2HealthCheck + ElectionTime + ClientReconnectWindow
   */
  public void printWarningIfL1L2FailureToleranceHigherThanL2L2FailureTolerance() {
    if (isL1L2FailureToleranceHigherThanL2L2FailureTolerance()) {
      logger
          .warn("High Availability Not Configured Properly: L1L2HealthCheck should be less than L2-L2HealthCheck + ElectionTime + ClientReconnectWindow");
    }
  }

  /*
   * To guard against the scenario described in DEV-6345 and FWS-32. In case of a new ACTIVE L2, this ensures that the
   * L1 clients attempt to connect to L2 only after the election is complete. Prints a warning if L1-L2HealthCheck <
   * L2-L2HealthCheck + ElectionTime
   */

  public void printWarningIfL1DisconnectIsLowerThanL2DisconnectPlusElectionTime() {
    if (isL1DisconnectIsLowerThanL2DisconnectPlusElectionTime()) {
      logger
          .warn("High Availability Not Configured Properly: L1L2HealthCheck should be more than L2-L2HealthCheck + ElectionTime");
    }
  }

  private boolean isL1L2FailureToleranceHigherThanL2L2FailureTolerance() {
    long l1MaxFailoverWindow = l2l2HealthCheckFailureTolerance + electionTime + clientReconnectWindow;
    return (l1l2HealthCheckFailureTolerance > l1MaxFailoverWindow);

  }

  private boolean isL1DisconnectIsLowerThanL2DisconnectPlusElectionTime() {
    long l1MinFailoverWindow = l2l2HealthCheckFailureTolerance + electionTime;
    return (l1l2HealthCheckFailureTolerance < l1MinFailoverWindow);
  }

  private int interNodeHealthCheckTime(int pingIdleTime, int socketConnectCount, int pingInterval, int pingProbes,
                                       int socketConnectTimeout) {
    // see http://www.terracotta.org/documentation/terracotta-server-array/high-availability
    return pingIdleTime + ((socketConnectCount) * (pingInterval * pingProbes + socketConnectTimeout * pingInterval));

  }

  private boolean checkIfHighAvailabilityIsEnabled(L2ConfigurationSetupManager config, TCProperties tcprops) {
    return (tcprops.getBoolean(TCPropertiesConsts.L1_HEALTHCHECK_L2_PING_ENABLED)
            && tcprops.getBoolean(TCPropertiesConsts.L2_HEALTHCHECK_L2_PING_ENABLED) && tcprops
        .getBoolean(TCPropertiesConsts.L2_L1RECONNECT_ENABLED));
  }
}
