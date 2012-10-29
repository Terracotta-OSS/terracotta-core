/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.rejoin;

public interface RejoinLifecycleListener {

  void onRejoinStart();

  void onRejoinComplete();

}
