/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.license.ProductID;

public interface ServerMessageChannelFactory {

  MessageChannelInternal createNewChannel(ChannelID id, ProductID productId);

}
