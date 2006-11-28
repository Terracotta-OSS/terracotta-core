package com.tc.net.protocol;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class NullSink implements GenericNetworkMessageSink {

  public void putMessage(GenericNetworkMessage msg) {
    return;
  }

}