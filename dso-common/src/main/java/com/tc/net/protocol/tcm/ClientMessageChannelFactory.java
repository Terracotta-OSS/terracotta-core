/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

public interface ClientMessageChannelFactory {

  MessageChannelInternal createNewChannel(String hostname, int port);

}
