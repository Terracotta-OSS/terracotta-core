/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.database.exceptions;

public class StatisticsDatabaseOpenErrorException extends StatisticsDatabaseException {
  private final String url;
  private final String user;
  private final String password;

  public StatisticsDatabaseOpenErrorException(final String url, final String user, final String password, final Throwable cause) {
    super("Can't connect to H2 database with URL '" + url + "', user '" + user + "' and password '" + password + "'", cause);
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }
}