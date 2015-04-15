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

import java.util.HashMap;
import java.util.Map;

public class GroupConfigBuilder extends BaseConfigBuilder {

  private final String      groupName;
  private L2ConfigBuilder[] l2s;
  private Integer           electionTime = null;

  public GroupConfigBuilder(String groupName) {
    super(5, new String[] { "election-time" });
    this.groupName = groupName;
  }

  public void setElectionTime(int value) {
    setProperty("election-time", value);
    this.electionTime = value;
  }

  @Override
  public String toString() {
    String out = "";

    Map attr = new HashMap();
    if (groupName != null) attr.put("group-name", groupName);
    if (electionTime != null) {
      attr.put("election-time", electionTime.toString());
    }

    out += openElement("mirror-group", attr);

    for (L2ConfigBuilder l2 : l2s) {
      out += l2.toString();
    }

    out += closeElement("mirror-group");

    return out;
  }

  public void setL2s(L2ConfigBuilder[] l2Builders) {
    l2s = l2Builders;
  }

  public L2ConfigBuilder[] getL2s() {
    return l2s;
  }
}
