/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MessageRecyclerImpl implements MessageRecycler {

  private static final int MAX_MESSAGES_TO_HOLD = 1000;

  private final LinkedList messages             = new LinkedList();
  private final Map        keys2RecycleItem     = new HashMap();

  public MessageRecyclerImpl() {
    super();
  }

  public synchronized void addMessage(DSOMessageBase message, Set keys) {
    if (!keys.isEmpty()) {
      final Set lkeys = new HashSet(keys.size());
      RecycleItem ri = new RecycleItem(message, lkeys);
      for (Iterator it = keys.iterator(); it.hasNext();) {
        Object key = it.next();
        lkeys.add(key);
        keys2RecycleItem.put(key, ri);
      }
      messages.addFirst(ri);
    } else {
      message.recycle();
    }
    if (messages.size() > MAX_MESSAGES_TO_HOLD) {
      // Let GC take care of it. We dont want a OOME !
      RecycleItem ri = (RecycleItem) messages.removeLast();
      remove(ri);
    }
  }

  private void remove(RecycleItem ri) {
    for (Iterator it = ri.getKeys().iterator(); it.hasNext();) {
      keys2RecycleItem.remove(it.next());
    }
  }

  public synchronized boolean recycle(Object key) {
    RecycleItem ri = (RecycleItem) keys2RecycleItem.remove(key);
    if (ri != null) {
      Set keys = ri.getKeys();
      keys.remove(key);
      if (keys.isEmpty()) {
        messages.remove(ri);
        DSOMessageBase message = ri.getMessage();
        message.recycle();
        return true;
      }
    }
    return false;
  }

  static final class RecycleItem {
    DSOMessageBase message;
    Set            keys;

    RecycleItem(DSOMessageBase message, Set keys) {
      this.message = message;
      this.keys = keys;
    }

    public DSOMessageBase getMessage() {
      return message;
    }

    public Set getKeys() {
      return keys;
    }
  }
}
