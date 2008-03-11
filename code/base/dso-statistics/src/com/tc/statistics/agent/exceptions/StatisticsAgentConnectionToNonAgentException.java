/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.agent.exceptions;

public class StatisticsAgentConnectionToNonAgentException extends StatisticsAgentConnectionException {
  public StatisticsAgentConnectionToNonAgentException() {
    super("The node that this connection is made to is not an active statistics agent.", null);
  }
}