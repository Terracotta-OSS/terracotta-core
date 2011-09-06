/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;

import java.util.HashMap;
import java.util.Map;

public class DummyStripeIDStateManager implements StripeIDStateManager {

  public StripeID getStripeID(GroupID gid) {
    return StripeID.NULL_ID;
  }

  public Map<GroupID, StripeID> getStripeIDMap(boolean fromAACoordinator) {
    return new HashMap();
  }

  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID) {
    return true;
  }

  public void registerForStripeIDEvents(StripeIDEventListener listener) {
    // NOP
  }

  public boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean overwrite) {
    return true;
  }

}
