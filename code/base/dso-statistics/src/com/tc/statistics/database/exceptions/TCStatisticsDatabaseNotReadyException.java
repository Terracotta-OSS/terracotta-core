/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

public class TCStatisticsDatabaseNotReadyException extends TCStatisticsDatabaseException {
  public TCStatisticsDatabaseNotReadyException() {
    super("Connection to database not established beforehand, call open() before performing another operation.", null);
  }
}
