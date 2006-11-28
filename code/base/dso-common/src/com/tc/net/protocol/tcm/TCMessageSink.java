package com.tc.net.protocol.tcm;

/**
 * Interface for classes that receive TC Messages
 * 
 * @author teck
 */
public interface TCMessageSink {
  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException;
}