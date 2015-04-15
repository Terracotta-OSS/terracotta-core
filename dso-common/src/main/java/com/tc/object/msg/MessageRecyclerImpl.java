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

  @Override
  public synchronized void addMessage(Recyclable message, Set keys) {
    if (!keys.isEmpty()) {
      final Set lkeys = new HashSet(keys.size());
      RecycleItem ri = new RecycleItem(message, lkeys);
      for (Iterator it = keys.iterator(); it.hasNext();) {
        Object key = it.next();
        lkeys.add(key);
        this.keys2RecycleItem.put(key, ri);
      }
      this.messages.addFirst(ri);
    } else {
      message.recycle();
    }
    if (this.messages.size() > MAX_MESSAGES_TO_HOLD) {
      // Let DGC take care of it. We don't want a OOME !
      RecycleItem ri = (RecycleItem) this.messages.removeLast();
      remove(ri);
    }
  }

  private void remove(RecycleItem ri) {
    for (Iterator it = ri.getKeys().iterator(); it.hasNext();) {
      this.keys2RecycleItem.remove(it.next());
    }
  }

  @Override
  public synchronized boolean recycle(Object key) {
    RecycleItem ri = (RecycleItem) this.keys2RecycleItem.remove(key);
    if (ri != null) {
      Set keys = ri.getKeys();
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

  static final class RecycleItem {
    Recyclable message;
    Set        keys;

    RecycleItem(Recyclable message, Set keys) {
      this.message = message;
      this.keys = keys;
    }

    public Recyclable getMessage() {
      return this.message;
    }

    public Set getKeys() {
      return this.keys;
    }
  }
}
