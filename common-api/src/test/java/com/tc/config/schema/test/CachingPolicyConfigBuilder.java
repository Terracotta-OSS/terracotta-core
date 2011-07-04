/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.test;

/**
 * Allows you to build valid config for a caching policy. This class <strong>MUST NOT</strong> invoke the actual XML
 * beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class CachingPolicyConfigBuilder extends BaseConfigBuilder {

  private final String       matchType;
  private final String       name;
  private final String       query;

  public static final String MATCH_TYPE_EXACT                  = "exact";
  public static final String MATCH_TYPE_REGEX                  = "regex";

  public static final String POLICY_LATEST_VALID               = "latest-valid";
  public static final String POLICY_STALE_DATA_ON_EXCEPTION    = "stale-data-on-exception";
  public static final String POLICY_CACHING_DISABLED           = "caching-disabled";
  public static final String POLICY_EMPTY_RESULTS_ON_EXCEPTION = "empty-results-on-exception";

  public CachingPolicyConfigBuilder(String matchType, String name, String query) {
    super(5, new String[0]);

    this.matchType = matchType;
    this.name = name;
    this.query = query;
  }

  public String toString() {
    String out = indent() + "<caching-policy";
    if (this.matchType != null) out += " match-type=\"" + this.matchType + "\"";
    if (this.name != null) out += " name=\"" + this.name + "\"";
    out += ">";
    if (this.query != null) out += this.query;
    out += "</caching-policy>\n";

    return out;
  }

}
