/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

public class StatisticsDatabaseCloseErrorException extends StatisticsDatabaseException {
  public StatisticsDatabaseCloseErrorException(final Throwable cause) {
    super("Unexpected error while closing the connection with the database.", cause);
  }
}