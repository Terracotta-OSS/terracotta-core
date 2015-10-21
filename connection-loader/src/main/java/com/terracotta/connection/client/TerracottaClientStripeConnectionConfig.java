/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.terracotta.connection.client;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.terracotta.connection.URLConfigUtil;


/**
 * Describes the servers, belonging to a single stripe, to which the client wishes to connect.
 */
public class TerracottaClientStripeConnectionConfig {
  private final List<String> stripeMembers;

  public TerracottaClientStripeConnectionConfig() {
    this.stripeMembers = new Vector<String>();
  }

  public void addStripeMemberUri(String member) {
    this.stripeMembers.add(member);
  }

  public List<String> getStripeMemberUris() {
    return Collections.unmodifiableList(this.stripeMembers);
  }

  public String getUsername() {
    // TODO:  Move this helper inside, instead of re-assembling the ,-delimited list.  This is just a stop-gap until a larger refactoring.
    String temp = null;
    for (String member : this.stripeMembers) {
      if (null == temp) {
        temp = member;
      } else {
        temp = temp + "," + member;
      }
    }
    return URLConfigUtil.getUsername(temp);
  }
}
