/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
