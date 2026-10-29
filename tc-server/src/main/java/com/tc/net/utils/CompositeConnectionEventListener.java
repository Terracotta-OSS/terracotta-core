/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.utils;

import com.tc.net.core.event.TCConnectionErrorEvent;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.core.event.TCConnectionEventListener;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Dispatches each connection event to several listeners, in the order they were given.
 *
 * <p>The connection layer stops notifying the remaining listeners when one of them throws, so listeners that must
 * not be skipped should be given first.
 */
public class CompositeConnectionEventListener implements TCConnectionEventListener {
  private final List<TCConnectionEventListener> listeners;

  public CompositeConnectionEventListener(TCConnectionEventListener... listeners) {
    this.listeners = asList(listeners);
  }

  @Override
  public void connectEvent(TCConnectionEvent event) {
    for (TCConnectionEventListener listener : listeners) {
      listener.connectEvent(event);
    }
  }

  @Override
  public void closeEvent(TCConnectionEvent event) {
    for (TCConnectionEventListener listener : listeners) {
      listener.closeEvent(event);
    }
  }

  @Override
  public void errorEvent(TCConnectionErrorEvent errorEvent) {
    for (TCConnectionEventListener listener : listeners) {
      listener.errorEvent(errorEvent);
    }
  }

  @Override
  public void endOfFileEvent(TCConnectionEvent event) {
    for (TCConnectionEventListener listener : listeners) {
      listener.endOfFileEvent(event);
    }
  }
}
