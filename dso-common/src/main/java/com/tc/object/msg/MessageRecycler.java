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
package com.tc.object.msg;

import com.tc.lang.Recyclable;

import java.util.Set;

public interface MessageRecycler {
  
  /*
   * adds a DSOMessage that needs to be recycled at a latter point in time, along with
   * the set of keys that needs to be processed before the message can be recycled. These
   * keys should be unique across the calls.
   */
  public void addMessage(Recyclable message, Set keys);
  
  /*
   * Indicates that the key is processed. The message associated with the key will be recycled
   * iff there are no more keys associated with it. 
   * 
   * @returns true if the Message associated with this key was recycled.
   */
  public boolean recycle(Object key);

}
