/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  private final LinkedList<RecycleItem<K>> messages             = new LinkedList<>();
  private final Map<K, RecycleItem<K>>     keys2RecycleItem     = new HashMap<>();

  public MessageRecyclerImpl() {
    super();
  }

  @Override
  public synchronized void addMessage(Recyclable message, Set<K> keys) {
    if (!keys.isEmpty()) {
      final Set<K> lkeys = new HashSet<>(keys.size());
      RecycleItem<K> ri = new RecycleItem<>(message, lkeys);
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
