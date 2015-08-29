/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.GroupID;
import com.tc.net.StripeID;

import java.util.Map;

public interface StripeIDStateManager {

  public boolean verifyOrSaveStripeID(GroupID gid, StripeID stripeID, boolean overwrite);

  public StripeID getStripeID(GroupID gid);

  public Map<GroupID, StripeID> getStripeIDMap(boolean fromAACoordinator);
  
  public boolean isStripeIDMatched(GroupID gid, StripeID stripeID);

  public void registerForStripeIDEvents(StripeIDEventListener listener);

}
