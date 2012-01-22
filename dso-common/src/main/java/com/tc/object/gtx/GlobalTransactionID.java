/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.util.AbstractIdentifier;

public class GlobalTransactionID extends AbstractIdentifier {

  public static final GlobalTransactionID NULL_ID = new GlobalTransactionID();

  public GlobalTransactionID(long id) {
    super(id);
  }

  private GlobalTransactionID() {
    super();
  }

  public String getIdentifierType() {
    return "GlobalTransactionID";
  }

  public boolean lessThan(GlobalTransactionID compare) {
    return isNull() ? true : toLong() < compare.toLong();
  }

  public GlobalTransactionID next() {
    return new GlobalTransactionID(toLong() + 1);
  }

}
