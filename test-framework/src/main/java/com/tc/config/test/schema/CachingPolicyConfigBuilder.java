/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.test.schema;

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

  @Override
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
