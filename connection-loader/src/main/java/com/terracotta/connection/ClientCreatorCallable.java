/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection;

import java.util.concurrent.Callable;

public interface ClientCreatorCallable extends Callable<Object> {

  String getUuid();
}
