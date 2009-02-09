/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster.exceptions;

public class UnclusteredObjectException extends RuntimeException {

  private final Object unclusteredObject;

  public UnclusteredObjectException(final Object unclusteredObject) {
    this.unclusteredObject = unclusteredObject;
  }

  public Object getUnclusteredObject() {
    return unclusteredObject;
  }
}