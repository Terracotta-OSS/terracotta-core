/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public interface TCObjectSelfStoreValue {
  // declare as TCObjectSelf instead of Object... skipped for tests... refactor later
  Object getTCObjectSelf();
}
