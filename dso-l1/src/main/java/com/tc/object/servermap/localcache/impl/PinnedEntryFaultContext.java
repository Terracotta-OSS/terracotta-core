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
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.servermap.localcache.PinnedEntryFaultCallback;

public class PinnedEntryFaultContext implements EventContext {

  private static final TCLogger          LOGGER = TCLogging.getLogger(PinnedEntryFaultContext.class);
  private final Object                   key;
  private final boolean                  eventual;
  private final PinnedEntryFaultCallback callback;

  public PinnedEntryFaultContext(Object key, boolean eventual, PinnedEntryFaultCallback callback) {
    this.key = key;
    this.eventual = eventual;
    this.callback = callback;
  }

  public void prefetch() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Going to prefetch entry : " + key);
    }
    if (eventual) {
      callback.unlockedGet(key);
    } else {
      callback.get(key);
    }
  }
}
