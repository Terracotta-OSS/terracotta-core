/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.util.AbstractIdentifier;

public class NetworkStackID extends AbstractIdentifier {

  public NetworkStackID(long id) {
    super(id);
  }
  
  public String getIdentifierType() {
    return "NetworkStackID";
  }

}
