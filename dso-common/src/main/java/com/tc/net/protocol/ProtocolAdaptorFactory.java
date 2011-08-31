/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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