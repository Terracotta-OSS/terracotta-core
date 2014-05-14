/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class TCCollections {

  public static final SortedSet   EMPTY_SORTED_SET    = Collections.unmodifiableSortedSet(new TreeSet());

  public static final ObjectIDSet EMPTY_OBJECT_ID_SET = new BasicObjectIDSet();
}
