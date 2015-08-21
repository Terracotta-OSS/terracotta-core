/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;


/**
 * Used by the comms layer to create protocol adaptor instances to hook up newly accept'ed socket connections 
 * 
 * @author teck
 */
public interface ProtocolAdaptorFactory {
  public TCProtocolAdaptor getInstance();
}
