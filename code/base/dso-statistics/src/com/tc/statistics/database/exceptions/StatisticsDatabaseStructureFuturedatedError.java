/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StatisticsDatabaseStructureFuturedatedError extends StatisticsDatabaseStructureMismatchError {
  public StatisticsDatabaseStructureFuturedatedError(final int actual, final int expected, final Date created) {
    super("The structure of the database is newer than what the software supports. It has version number "+actual+", while version "+expected+" was expected. It was created on "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(created)+".", actual, expected, created);
  }
}