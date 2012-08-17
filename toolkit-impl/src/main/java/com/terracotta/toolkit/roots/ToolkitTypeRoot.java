/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.roots;

import com.terracotta.toolkit.object.TCToolkitObject;

/**
 * A Terracotta Root. <tt>ClusteredObject</tt>s added to this root are added to the clustered object graph of this root
 * and becomes available in the cluster
 */
public interface ToolkitTypeRoot<T extends TCToolkitObject> {

  /**
   * Adds a <tt>ClusteredObject</tt> to this root, identifiable by <tt>name</tt>
   */
  void addClusteredObject(String name, T clusteredObject);

  /**
   * Gets a <tt>ClusteredObject</tt> identified by <tt>name</tt>
   */
  T getClusteredObject(String name);

  /**
   * Removes the <tt>ClusteredObject</tt> identified by <tt>name</tt> from this root
   */
  void removeClusteredObject(String name);

}
