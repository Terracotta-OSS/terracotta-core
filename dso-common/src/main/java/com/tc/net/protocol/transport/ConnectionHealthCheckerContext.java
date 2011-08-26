/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

/**
 * A Context per Transport takes care of sending and receiving the health checking probe signals to peer nodes. Also the
 * extra checks like socket connect to detect Long GC.
 *
 * @author Manoj
 */
public interface ConnectionHealthCheckerContext {

  /* Transport is lively */
  public void refresh();

  /* Probe Message send and receive */
  public boolean probeIfAlive();

  public boolean receiveProbe(HealthCheckerProbeMessage message);

}
