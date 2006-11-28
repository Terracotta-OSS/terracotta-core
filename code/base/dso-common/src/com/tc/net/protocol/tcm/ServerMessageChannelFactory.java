/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ServerMessageChannelFactory {

  MessageChannelInternal createNewChannel(ChannelID id);

  TCMessageFactory getMessageFactory();

  TCMessageRouter getMessageRouter();

}
