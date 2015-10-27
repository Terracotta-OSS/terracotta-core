/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.async.api;

import com.tc.stats.Monitorable;

/**
 * Represents the sink in the SEDA system
 */
public interface Sink<EC> extends Monitorable {
  /**
   * Add the given event to the internal queue 0 (that is, it won't be executed concurrently with any other contexts passed
   * in this way.
   * 
   * @param context
   */
  public void addSingleThreaded(EC context);

  /**
   * Adds an event to the queue for multi-threaded execution (that is, it could be executed concurrently with other contexts
   * pass in this way or even relative to single-threaded contexts).
   * 
   * TODO:  Split out the multi-threaded cases to avoid the casts required in the implementations.  This hack is just a
   * stop-gap to keep the change to add types to SEDA smaller.
   * 
   * @param context
   */
  public void addMultiThreaded(EC context);

  /**
   * Adds a specialized event to the queue for multi-threaded execution.
   * Specialized events differ from the normal kind in 2 ways:
   * 1) They are always multi-threaded (the inherit from that class)
   * 2) They are NOT invoked by the handler, but directly "execute()"-ed.
   * 
   * @param specialized The specialized context to execute().
   */
  public void addSpecialized(SpecializedEventContext specialized);

  /**
   * returns the current size of the queue
   * 
   * @return
   */
  public int size();

  public void clear();

}
