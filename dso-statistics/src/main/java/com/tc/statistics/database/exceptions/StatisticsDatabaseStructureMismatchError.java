/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

import java.util.Date;

public class StatisticsDatabaseStructureMismatchError extends Error {
  private final int actual;
  private final int expected;
  private final Date created;

  public StatisticsDatabaseStructureMismatchError(final String message, final int actual, final int expected, final Date created) {
    super(message);
    this.actual = actual;
    this.expected = expected;
    this.created = created;
  }

  public int getActualVersion() {
    return actual;
  }

  public int getExpectedVersion() {
    return expected;
  }

  public Date getCreationDate() {
    return created;
  }
}