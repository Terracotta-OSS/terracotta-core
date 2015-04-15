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
package com.terracotta.toolkit.type;

import org.terracotta.toolkit.config.Configuration;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;

public interface IsolatedToolkitTypeFactory<T extends RejoinAwareToolkitObject, S extends TCToolkitObject> {

  /**
   * Used to create the unclustered type after faulting in the TCClusteredObject
   */
  T createIsolatedToolkitType(ToolkitObjectFactory<T> factory, IsolatedClusteredObjectLookup<S> lookup, String name,
                              Configuration config, S tcClusteredObject);

  /**
   * Used to create the TCClusteredObject to back the type
   */
  S createTCClusteredObject(Configuration config);

}
