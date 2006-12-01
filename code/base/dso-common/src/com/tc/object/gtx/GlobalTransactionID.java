/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.util.AbstractIdentifier;

import java.util.Comparator;

public class GlobalTransactionID extends AbstractIdentifier {

  public static final GlobalTransactionID NULL_ID    = new GlobalTransactionID();
  public static final Comparator          COMPARATOR = new Comparator() {
                                                       public int compare(Object o1, Object o2) {
                                                         long l1 = ((GlobalTransactionID) o1).toLong();
                                                         long l2 = ((GlobalTransactionID) o2).toLong();
                                                         if (l1 < l2) return -1;
                                                         else if (l1 > l2) return 1;
                                                         else return 0;
                                                       }
                                                     };

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

}
