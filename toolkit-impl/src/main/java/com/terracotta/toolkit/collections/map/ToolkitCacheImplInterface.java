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
package com.terracotta.toolkit.collections.map;

import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.store.ToolkitStore;

/**
 * This interface is needed so that we can create a Proxy Object for ToolkitCacheImpl.
 */
public interface ToolkitCacheImplInterface<K, V> extends ToolkitStore<K, V>, ToolkitCacheInternal<K, V>, BufferingToolkitCache<K, V> {
  // No additional methods
}
