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
package com.tc.object.msg;

import com.tc.lang.Recyclable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MessageRecyclerImpl<K> implements MessageRecycler<K> {

  private static final int              MAX_MESSAGES_TO_HOLD = 1000;

  private final LinkedList<RecycleItem<K>> messages             = new LinkedList<RecycleItem<K>>();
  private final Map<K, RecycleItem<K>>     keys2RecycleItem     = new HashMap<K, RecycleItem<K>>();

  public MessageRecyclerImpl() {
    super();
  }

  @Override
  public synchronized void addMessage(Recyclable message, Set<K> keys) {
    if (!keys.isEmpty()) {
      final Set<K> lkeys = new HashSet<K>(keys.size());
      RecycleItem<K> ri = new RecycleItem<K>(message, lkeys);
      for (K key : keys) {
        lkeys.add(key);
        this.keys2RecycleItem.put(key, ri);
      }
      this.messages.addFirst(ri);
    } else {
      message.recycle();
    }
    if (this.messages.size() > MAX_MESSAGES_TO_HOLD) {
      // Let DGC take care of it. We don't want a OOME !
      RecycleItem<K> ri = this.messages.removeLast();
      remove(ri);
    }
  }

  private void remove(RecycleItem<K> ri) {
    for (K key : ri.getKeys()) {
      this.keys2RecycleItem.remove(key);
    }
  }

  @Override
  public synchronized boolean recycle(K key) {
    RecycleItem<K> ri = this.keys2RecycleItem.remove(key);
    if (ri != null) {
      Set<K> keys = ri.getKeys();
      keys.remove(key);
      if (keys.isEmpty()) {
        this.messages.remove(ri);
        Recyclable message = ri.getMessage();
        message.recycle();
        return true;
      }
    }
    return false;
  }

  static final class RecycleItem<K> {
    private final Recyclable message;
    private final Set<K>        keys;

    RecycleItem(Recyclable message, Set<K> keys) {
      this.message = message;
      this.keys = keys;
    }

     Recyclable getMessage() {
      return this.message;
    }

     Set<K> getKeys() {
      return this.keys;
    }
  }
}
