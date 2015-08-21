/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.ClientID;

public interface ClientIDProvider {
  
  public ClientID getClientID();

}
