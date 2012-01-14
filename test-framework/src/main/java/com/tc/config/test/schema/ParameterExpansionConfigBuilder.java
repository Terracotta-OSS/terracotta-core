/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.test.schema;

/**
 * Allows you to build valid config for a parameter expansion entry. This class <strong>MUST NOT</strong> invoke the
 * actual XML beans to do its work; one of its purposes is, in fact, to test that those beans are set up correctly.
 */
public class ParameterExpansionConfigBuilder extends BaseConfigBuilder {

  private final String matchType;
  private final String expandParameters;
  private final String query;

  public static final String MATCH_TYPE_EXACT = "exact";
  public static final String MATCH_TYPE_REGEX = "regex";

  public ParameterExpansionConfigBuilder(String matchType, String expandParameters, String query) {
    super(5, new String[0]);

    this.matchType = matchType;
    this.expandParameters = expandParameters;
    this.query = query;
  }

  public String toString() {
    String out = indent() + "<parameter-expansion";
    if (this.matchType != null) out += " match-type=\"" + this.matchType + "\"";
    if (this.expandParameters != null) out += " expand-parameters-in-positions=\"" + this.expandParameters+ "\"";
    out += ">";
    if (this.query != null) out += this.query;
    out += "</parameter-expansion>\n";

    return out;
  }

}
