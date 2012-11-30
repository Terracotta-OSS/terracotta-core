/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.rejoin;

public interface RejoinCallback {

  void rejoinStarted();

  void rejoinCompleted();
}
