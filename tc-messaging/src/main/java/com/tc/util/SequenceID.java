/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.Comparator;

public class SequenceID extends AbstractIdentifier {

  public static final SequenceID NULL_ID    = new SequenceID();
  public static final Comparator<SequenceID> COMPARATOR = new SequenceIDComparator();

  public SequenceID(long l) {
    super(l);
  }

  private SequenceID() {
    return;
  }

  @Override
  public String getIdentifierType() {
    return "SequenceID";
  }

  public SequenceID next() {
    return new SequenceID(toLong() + 1);
  }

  public static class SequenceIDComparator implements Comparator<SequenceID> {
    @Override
    public int compare(SequenceID id1, SequenceID id2) {
      long l1 = id1.toLong();
      long l2 = id2.toLong();
      if (l1 < l2) return -1;
      else if (l1 > l2) return 1;
      else return 0;
    }
  }

}
