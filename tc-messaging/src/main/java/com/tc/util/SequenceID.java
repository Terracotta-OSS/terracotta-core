/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
