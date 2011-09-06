/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store.exceptions;

public class StatisticsStoreClearStatisticsErrorException extends StatisticsStoreException {
  private final String sessionId;

  public StatisticsStoreClearStatisticsErrorException(final String sessionId, final Throwable cause) {
    super("Unexpected error while clearing the statistics for session ID '" + sessionId +  "'.", cause);
     this.sessionId = sessionId;
   }

   public String getSessionId() {
     return sessionId;
   }
}