/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.platform.rejoin;

import com.tc.net.protocol.tcm.MessageChannel;

public interface RejoinManagerInternal extends RejoinManager {
  void doRejoin(MessageChannel channel);
}
