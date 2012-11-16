/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.tests.base;

public interface TestFailureListener {
  void testFailed(String reason);
}
