/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util.diff;


/**
 * An object that can compute a list of differences between it and another object.
 */
public interface Differenceable {
  
  void addDifferences(DifferenceContext context, Object that);

}
