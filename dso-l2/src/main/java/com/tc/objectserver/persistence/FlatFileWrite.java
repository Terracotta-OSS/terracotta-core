/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.objectserver.persistence;

import java.util.concurrent.Callable;

/**
 *
 */
public interface FlatFileWrite {
  <T> T run(Callable<T> r);
}
