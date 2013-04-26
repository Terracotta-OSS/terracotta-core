/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.google.common.eventbus.Subscribe;

/**
 * Generic listener interface for server events.
 * To listen for a particular type of event see {@link TypedInterestListenerSupport}.
 *
 * @author Eugene Shelestovich
 * @see TypedInterestListenerSupport
 * @see Interest
 */
public interface InterestListener {

  @Subscribe
  void onInterest(Interest interest);
}
