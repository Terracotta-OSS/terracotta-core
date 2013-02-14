/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.util.collections;

import java.util.concurrent.Callable;

public interface OnGCCallable {
  Callable<Void> onGCCallable();
}
