/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Collections;

public class NullCache implements EvictionPolicy {

  public boolean add(final Cacheable obj) {
    return false;
  }

  public Collection getRemovalCandidates(final int maxCount) {
    return Collections.EMPTY_LIST;
  }

  public void remove(final Cacheable obj) {
    // nothing to remove. The cache is null
  }

  public void markReferenced(final Cacheable obj) {
    // move the referenced object up in the lru
  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    return out.println("NULL CACHE");
  }

  public int getCacheCapacity() {
    return Integer.MAX_VALUE;
  }
}
