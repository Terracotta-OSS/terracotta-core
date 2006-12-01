/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
