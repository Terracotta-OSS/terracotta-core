/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import java.util.concurrent.Callable;

public interface ClientCreatorCallable extends Callable<Object> {
  Object getAbortableOperationManager();

  String getUuid();
}
