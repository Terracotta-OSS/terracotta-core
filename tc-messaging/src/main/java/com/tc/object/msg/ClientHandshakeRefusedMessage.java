/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;

public interface ClientHandshakeRefusedMessage extends TCMessage {
  String getRefualsCause();

  void initialize(String message);

}
