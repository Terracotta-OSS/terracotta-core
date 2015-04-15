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
package com.terracotta.toolkit.nonstop;

public interface NonStopManager {

  /**
   * begin a non stop operation with a timeout. Throws IllegalStateException if a nonstop operation is already started
   * for this thread
   */
  void begin(long timeout);

  /**
   * try to begin a non stop operation with a timeout. retruns false if a nonstop operation was already started
   */
  boolean tryBegin(long timeout);

  /**
   * Indicate that the non stop operation completed
   */
  void finish();

}