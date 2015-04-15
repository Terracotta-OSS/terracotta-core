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

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.roots.impl.AggregateIsolatedToolkitTypeRoot;
import com.terracotta.toolkit.type.DistributedToolkitType;
import com.terracotta.toolkit.type.DistributedToolkitTypeFactory;
import com.terracotta.toolkit.type.IsolatedToolkitTypeFactory;

/**
 * A factory responsible for creating different types of roots
 */
public interface ToolkitTypeRootsFactory {

  <T extends RejoinAwareToolkitObject, S extends TCToolkitObject> AggregateIsolatedToolkitTypeRoot<T, S> createAggregateIsolatedTypeRoot(String name,
                                                                                                                              IsolatedToolkitTypeFactory<T, S> factory,
                                                                                                                              PlatformService platformService);

  <T extends DistributedToolkitType<S>, S extends TCToolkitObject> AggregateToolkitTypeRoot<T, S> createAggregateDistributedTypeRoot(String rootName,
                                                                                                                                     DistributedToolkitTypeFactory<T, S> factory,
                                                                                                                                     PlatformService platformService);

}
