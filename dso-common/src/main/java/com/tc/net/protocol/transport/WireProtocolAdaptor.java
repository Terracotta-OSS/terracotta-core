/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.protocol.TCProtocolAdaptor;

public interface WireProtocolAdaptor extends TCProtocolAdaptor {
  // I put this here during a refactoring to test ServerStackProvider. It doesn't seem to have much to do, except to be
  // a placeholder in the type hierarchy. --Orion 12/15/05
}