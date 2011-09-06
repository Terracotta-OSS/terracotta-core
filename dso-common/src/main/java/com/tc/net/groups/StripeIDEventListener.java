/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.StripeID;

/*
 * Listen to the StripeID events
 */
public interface StripeIDEventListener {

  public void notifyStripeIDCreated(StripeID stripeID);

  public void notifyStripeIDMapReady();

}
