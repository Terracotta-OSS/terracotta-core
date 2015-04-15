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
package com.tc.runtime.cache;

public final class CacheMemoryEventType {

  public static final CacheMemoryEventType ABOVE_THRESHOLD          = new CacheMemoryEventType("ABOVE_THRESHOLD");
  public static final CacheMemoryEventType ABOVE_CRITICAL_THRESHOLD = new CacheMemoryEventType("ABOVE_CRITICAL_THRESHOLD");
  public static final CacheMemoryEventType BELOW_THRESHOLD          = new CacheMemoryEventType("BELOW_THRESHOLD");

  private final String                name;

  // Not exposed to anyone
  private CacheMemoryEventType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "CacheMemoryEventType." + name;
  }

}
