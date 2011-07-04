/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

/**
 * Allows you to build valid config for a query route. This class <strong>MUST NOT</strong> invoke the actual XML beans
 * to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class QueryRouteConfigBuilder extends BaseConfigBuilder {

  private final String       matchType;
  private final String       route;
  private final String       query;

  public static final String MATCH_TYPE_EXACT = "exact";
  public static final String MATCH_TYPE_REGEX = "regex";

  public static final String ROUTE_TERRACOTTA = "terracotta";
  public static final String ROUTE_NATIVE     = "native";

  public QueryRouteConfigBuilder(String matchType, String route, String query) {
    super(5, new String[0]);

    this.matchType = matchType;
    this.route = route;
    this.query = query;
  }

  public String toString() {
    String out = indent() + "<query-route";
    if (this.matchType != null) out += " match-type=\"" + this.matchType + "\"";
    if (this.route != null) out += " route=\"" + this.route + "\"";
    out += ">";
    if (this.query != null) out += this.query;
    out += "</query-route>\n";

    return out;
  }

}
