/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class TCCollections {

  public static final SortedSet   EMPTY_SORTED_SET    = Collections.unmodifiableSortedSet(new TreeSet());

  public static final ObjectIDSet EMPTY_OBJECT_ID_SET = new EmptyObjectIDSet();

  private static final class EmptyObjectIDSet extends ObjectIDSet {

    @Override
    public boolean add(ObjectID id) {
      throw new UnsupportedOperationException();
    }

    // Preserves singleton property
    private Object readResolve() {
      return EMPTY_OBJECT_ID_SET;
    }

  }

}
