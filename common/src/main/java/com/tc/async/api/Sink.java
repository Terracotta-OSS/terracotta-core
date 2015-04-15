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
package com.tc.async.api;

import com.tc.stats.Monitorable;

import java.util.Collection;

/**
 * Represents the sink in the SEDA system
 */
public interface Sink extends Monitorable {
  /**
   * The context may or may not be added to the sink depending on the state of the sink. The implementation can make the
   * decision based on various factors.
   * 
   * @param context
   * @return
   */
  public boolean addLossy(EventContext context);

  /**
   * Add More than one context at a time. This is more efficient then adding one at a time
   * 
   * @param contexts
   */
  public void addMany(Collection contexts);

  /**
   * Add a event to the Sink (no, really!)
   * 
   * @param context
   */
  public void add(EventContext context);

  /**
   * The predicate allows the Sink to reject the EventContext rather than handle it
   * 
   * @param predicate
   */
  public void setAddPredicate(AddPredicate predicate);

  /**
   * Get the predicate 
   * 
   */
  public AddPredicate getPredicate();

  /**
   * returns the current size of the queue
   * 
   * @return
   */
  public int size();

  public void clear();

}
