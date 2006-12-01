/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util;

import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;

import java.util.HashMapTC.ValueWrapper;

/*
 * This class is merged with java.util.LinkedHashMap in the bootjar. Since HashMapTC will also be merged with
 * java.util.HashMap, this class will inherit all behavior of HashMapTC including the Manageable methods and
 * Clearable method. It is declared abstract to make the compiler happy
 */
public abstract class LinkedHashMapTC extends LinkedHashMap implements Manageable {

  // XXX:: Its dumb to store two copies of this
  private final boolean accessOrder;

  public LinkedHashMapTC() {
    super();
    accessOrder = false;
  }

  public LinkedHashMapTC(int initialCapacity) {
    super(initialCapacity);
    accessOrder = false;
  }

  public LinkedHashMapTC(Map arg0) {
    super(arg0);
    accessOrder = false;
  }

  public LinkedHashMapTC(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
    accessOrder = false;
  }

  public LinkedHashMapTC(int initialCapacity, float loadFactor, boolean accessOrder) {
    super(initialCapacity, loadFactor, accessOrder);
    this.accessOrder = accessOrder;
  }

  // This is pretty much a C&P of the one in HashMapTC
  public boolean containsValue(Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        if (value != null) {
          // XXX:: This is tied closely to HashMap implementation which calls equals on the passed value rather than the
          // other way around
          return super.containsValue(new ValueWrapper(value));
        } else {
          return super.containsValue(value);
        }
      }
    } else {
      return super.containsValue(value);
    }
  }

  public Object get(Object key) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        Object val = super.get(key);
        if (accessOrder) {
          ManagerUtil.checkWriteAccess(this);
          ManagerUtil.logicalInvoke(this, "get(Ljava/lang/Object;)Ljava/lang/Object;", new Object[] { key });
        }
        return lookUpAndStoreIfNecessary(key, val);
      }
    } else {
      return super.get(key);
    }
  }

  private Object lookUpAndStoreIfNecessary(Object key, Object value) {
    if (value instanceof ObjectID) {
      Object newVal = ManagerUtil.lookupObject((ObjectID) value);
      Map.Entry e = getEntry(key);
      // e should not be null here
      e.setValue(newVal);
      return newVal;
    } else {
      return value;
    }
  }

}
