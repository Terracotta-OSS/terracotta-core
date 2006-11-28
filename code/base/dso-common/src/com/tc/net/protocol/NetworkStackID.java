/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.util.AbstractIdentifier;

public class NetworkStackID extends AbstractIdentifier {

  private NetworkStackID()  {
    super();
  }
  
  public NetworkStackID(long id) {
    super(id);
  }
  
  public String getIdentifierType() {
    return "NetworkStackID";
  }

}
