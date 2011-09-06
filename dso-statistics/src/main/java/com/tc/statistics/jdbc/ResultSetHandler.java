/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetHandler {
  public void useResultSet(ResultSet resultSet) throws SQLException;
}