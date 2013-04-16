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
  void onPut(PutInterest interest);

  @Subscribe
  void onRemove(RemoveInterest interest);

  @Subscribe
  void onEviction(EvictionInterest interest);

  @Subscribe
  void onExpiration(ExpirationInterest interest);

}
