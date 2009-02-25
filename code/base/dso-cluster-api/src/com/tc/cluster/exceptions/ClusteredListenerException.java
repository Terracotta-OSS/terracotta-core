/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.cluster.exceptions;

import com.tc.cluster.DsoClusterListener;

public class ClusteredListenerException extends RuntimeException {

  private final DsoClusterListener listener;

  public ClusteredListenerException(final DsoClusterListener listener) {
    this.listener = listener;
  }

  public DsoClusterListener getClusteredListener() {
    return listener;
  }
}