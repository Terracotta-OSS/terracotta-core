/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
  void refresh();

  /* Probe Message send and receive */
  boolean probeIfAlive();

  boolean receiveProbe(HealthCheckerProbeMessage message);

  void checkTime();
}
