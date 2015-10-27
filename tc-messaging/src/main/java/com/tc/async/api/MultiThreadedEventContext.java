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

/**
 * This interface must be implemented by events which want to be processed in multi-threaded queues.  It also allows the
 * implementor a way to describe how it should be scheduled by means of the scheduling key.
 */
public interface MultiThreadedEventContext {
  /**
   * The scheduling key is an object used to determine how to schedule this event in a multi-threaded queue.
   * Events which return the same key (equals()-equality) will be handled in the order they arrive, with one completing
   * before the next starts.
   * No guarantees are made regarding the relative order in which events with different keys are handled.  This means that
   * they may run in any relative order and also concurrently.
   * Note that returning null for a scheduling key means that no special accommodation will be made for the event, and it
   * will be executed in an implementation-defined default ordering (typically, this means an optimistic scheduling in the
   * shortest queue). 
   * 
   * @return The key for scheduling (null in the case of "any scheduling").
   */
  Object getSchedulingKey();
  /**
   * Flush all the threads of the multi-threaded scheduler that this event context is executed on.  When flush returns true,
   * all events scheduled before this one will have been fully executed before this event is processed.
   * @return true if a flush is requested
   */
  boolean flush();
}
