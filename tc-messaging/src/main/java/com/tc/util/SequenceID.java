/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
