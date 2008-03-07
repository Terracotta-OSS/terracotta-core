/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

public class TCStatisticsDatabaseStatementPreparationErrorException extends TCStatisticsDatabaseException {
  private final String sql;

  public TCStatisticsDatabaseStatementPreparationErrorException(final String sql, final Throwable cause) {
    super("Unexpected error while preparing a statement for SQL '"+sql+"'.", cause);
    this.sql = sql;
  }

  public String getSql() {
    return sql;
  }
}