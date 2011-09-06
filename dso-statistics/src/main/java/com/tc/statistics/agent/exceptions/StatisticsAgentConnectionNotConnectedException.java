/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.exceptions;

public class StatisticsAgentConnectionNotConnectedException extends StatisticsAgentConnectionException {
  public StatisticsAgentConnectionNotConnectedException() {
    super("The statistics agent is not connected.", null);
  }
}