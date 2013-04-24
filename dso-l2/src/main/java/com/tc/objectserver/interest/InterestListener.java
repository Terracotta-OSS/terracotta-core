/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.google.common.eventbus.Subscribe;

/**
 * @author Eugene Shelestovich
 */
public interface InterestListener {

  @Subscribe
  void onInterest(Interest interest);
}
