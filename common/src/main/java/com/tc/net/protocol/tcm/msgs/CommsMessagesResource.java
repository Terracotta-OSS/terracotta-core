/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm.msgs;

import java.util.ResourceBundle;

 class CommsMessagesResource {
  
  private static final CommsMessagesResource instance = new CommsMessagesResource();
  private final ResourceBundle resources;

  private CommsMessagesResource() {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".comms");
  }
  
  static String getL2L1RejectionMessage(){
    return instance.resources.getString("l2.l1.reject");
  }
  
  static String getL2L2RejectionMessage(){
    return instance.resources.getString("l2.l2.reject");
  }

}
