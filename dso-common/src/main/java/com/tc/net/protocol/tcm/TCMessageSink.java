/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

/**
 * Interface for classes that receive TC Messages
 * 
 * @author teck
 */
public interface TCMessageSink {
  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException;
}