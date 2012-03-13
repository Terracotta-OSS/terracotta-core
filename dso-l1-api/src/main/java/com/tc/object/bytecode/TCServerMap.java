/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

public interface TCServerMap extends Manageable {

  public void evictedInServer(boolean notifyEvicted, Object key);
}
