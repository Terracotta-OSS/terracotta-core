/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

/**
 * Class for helping {@link TCObjectSelfImpl} compile.
 * <p/>
 * Methods implemented by this method should throw exception, and are not supposed to be implemented. Those methods are
 * defined here which are not implemented/defined in TCObjectSelfImpl. Classes that dynamically merges
 * {@link TCObjectSelfImpl} to itself should provide the implementation, e.g. SerializedEntry <br/>
 * These methods are defined here so that {@link TCObjectSelfImpl} does not need provide any definition of these
 * methods, as those defined there are merged to the target classes
 */
public abstract class TCObjectSelfCompilationHelper implements TCObjectSelf {

  public int touch() {
    throw new UnsupportedOperationException();
  }

  public int untouch() {
    throw new UnsupportedOperationException();
  }

}
