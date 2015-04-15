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

  @Override
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
