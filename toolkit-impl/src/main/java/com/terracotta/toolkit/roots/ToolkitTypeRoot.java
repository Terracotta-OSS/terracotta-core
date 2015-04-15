/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
