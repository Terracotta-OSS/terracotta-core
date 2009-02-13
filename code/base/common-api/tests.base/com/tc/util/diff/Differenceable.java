/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.diff;


/**
 * An object that can compute a list of differences between it and another object.
 */
public interface Differenceable {
  
  void addDifferences(DifferenceContext context, Object that);

}
