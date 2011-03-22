/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;

import java.util.HashMap.Entry;
import java.util.HashMapTC.ValueWrapper;

/*
 * This class is merged with java.util.LinkedHashMap in the bootjar. Since HashMapTC will also be merged with
 * java.util.HashMap, this class will inherit all behavior of HashMapTC including the Manageable methods and Clearable
 * method. It is declared abstract to make the compiler happy
 */
public abstract class LinkedHashMapTC extends LinkedHashMap implements Manageable {

  private boolean accessOrder;

  // This is pretty much a C&P of the one in HashMapTC
  @Override
  public boolean containsValue(Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        if (value != null) {
          // XXX:: This is tied closely to HashMap implementation which calls equals on the passed value rather than the
          // other way around
          return super.containsValue(new ValueWrapper(value));
        } else {
          return super.containsValue(null);
        }
      }
    } else {
      return super.containsValue(value);
    }
  }

  @Override
  public Object get(Object key) {
    if (__tc_isManaged()) {
      Entry entry = null;
      synchronized (__tc_managed().getResolveLock()) {
        if (accessOrder) {
          ManagerUtil.checkWriteAccess(this);
        }

        // XXX: doing two lookups here!!
        entry = super.getEntry(key);
        if (entry == null) { return null; }

        Object actualKey = entry.getKey();

        // do the original get logic
        super.get(key);

        if (accessOrder) {
          ManagerUtil.logicalInvoke(this, "get(Ljava/lang/Object;)Ljava/lang/Object;", new Object[] { actualKey });
        }
      }
      return lookUpAndStoreIfNecessary(entry);
    } else {
      return super.get(key);
    }
  }

  private Object lookUpAndStoreIfNecessary(Map.Entry e) {
    if (e == null) return null;
    Object value = e.getValue();
    Object resolvedValue = lookUpIfNecessary(value);
    __tc_storeValueIfValid(e, resolvedValue);
    return resolvedValue;
  }

  // This method name needs to be prefix with __tc_ in order to prevent it from being
  // autolocked.
  private void __tc_storeValueIfValid(Map.Entry preLookupEntry, Object resolvedValue) {
    synchronized (__tc_managed().getResolveLock()) {
      Map.Entry postLookupEntry = getEntry(preLookupEntry.getKey());
      if (postLookupEntry != null && preLookupEntry.getValue() == postLookupEntry.getValue()
          && resolvedValue != preLookupEntry.getValue()) {
        preLookupEntry.setValue(resolvedValue);
      }
    }
  }

  private static Object lookUpIfNecessary(Object o) {
    if (o instanceof ObjectID) {
      try {
        return ManagerUtil.lookupObject((ObjectID) o);
      } catch (TCObjectNotFoundException onfe) {
        throw new ConcurrentModificationException(onfe.getMessage());
      }
    }
    return o;
  }

}
