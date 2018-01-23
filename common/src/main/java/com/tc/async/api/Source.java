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

import com.tc.async.impl.Event;

/**
 * This is used by the internals to manage the process of processing EventContexts in the manner that makes sense for
 * each one. Individual Stages SHOULD NOT HAVE TO EITHER USE OR IMPLEMENT THIS INTERFACE
 * 
 */

public interface Source {

  public Event poll(long period) throws InterruptedException;

  public String getSourceName();

  public boolean isEmpty();
  
  public int size();
}
