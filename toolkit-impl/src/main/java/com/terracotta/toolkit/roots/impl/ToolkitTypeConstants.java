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
package com.terracotta.toolkit.roots.impl;

public abstract class ToolkitTypeConstants {

  private static final String PREFIX                           = "__toolkit@";

  public static final String  TOOLKIT_LIST_ROOT_NAME           = PREFIX + "toolkitListRoot";
  public static final String  TOOLKIT_MAP_ROOT_NAME            = PREFIX + "toolkitMapRoot";
  public static final String  TOOLKIT_SORTED_MAP_ROOT_NAME     = PREFIX + "toolkitSortedMapRoot";
  public static final String  TOOLKIT_STORE_ROOT_NAME          = PREFIX + "toolkitStoreRoot";
  public static final String  TOOLKIT_CACHE_ROOT_NAME          = PREFIX + "toolkitCacheRoot";
  public static final String  TOOLKIT_ATOMIC_LONG_MAP_NAME     = PREFIX + "toolkitAtomicLongMap";
  public static final String  TOOLKIT_BARRIER_MAP_NAME         = PREFIX + "toolkitBarrierMap";
  public static final String  TOOLKIT_NOTIFIER_ROOT_NAME       = PREFIX + "toolkitNotifierRoot";
  public static final String  TOOLKIT_SET_ROOT_NAME            = PREFIX + "toolkitSetRoot";
  public static final String  SERIALIZER_MAP_ROOT_NAME         = PREFIX + "serializerMapRoot";
  public static final String  TOOLKIT_BLOCKING_QUEUE_ROOT_NAME = PREFIX + "toolkitBlockingQueueRoot";
  public static final String  TOOLKIT_SORTED_SET_ROOT_NAME     = PREFIX + "toolkitSortedSetRoot";
  public static final String  TOOLKIT_BARRIER_UID_NAME         = PREFIX + "__tc__toolkit_barrier_uid@";
  public static final String  TOOLKIT_LONG_UID_NAME            = PREFIX + "__tc__toolkit_long_uid@";

}
