/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package java.util;

import com.tc.exception.TCObjectNotFoundException;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObject;
import com.tc.object.TCObjectExternal;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.CloneUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections.SynchronizedCollection;
import java.util.Collections.SynchronizedSet;
import java.util.Map.Entry;

/*
 * This class will be merged with java.lang.Hashtable in the bootjar. This hashtable can store ObjectIDs instead of
 * Objects to save memory and transparently fault Objects as needed. It can also clear references. For General rules
 * @see HashMapTC class The original implementation of HashtableTC methods does not contain synchronized
 * (__tc_managed().getResolveLock()). This is because methods are already synchronized and thus could act as the common
 * memory barrier between the application and the applicator thread. After we remove local jvm lock for read level
 * autolock, when Hashtable is read autolocked, there is no longer a common barrier between application and the
 * applicator thread. To establish a common barrier between the two, we need to add synchronized
 * (__tc_managed().getResolveLock()). The locking order must be locking on Hashtable followed by
 * __tc_managed().getResolveLock(); otherwise, it is possible that an application thread is holding a resolveLock, while
 * waiting for a dso lock, thus, preventing the applicator thread from applying the transaction and ack. On the other
 * hand, since the applicator thread is already synchronize on __tc_managed().getResolveLock(), its locking order is
 * different from that in an application thread; hence, deadlock may result. To solve this problem, methods
 * __tc_applicator_put(), __tc_applicator_remove(), and __tc_applicator_clear() are introduced. These applicator
 * specific methods do not need to be synchronized on __tc_managed().getResolveLock() as they are only being called by
 * the applicator and the applicator is already synchronized on __tc_managed().getResolveLock(). These methods do not
 * need to be synchronized on the Hashtable either as the common memory barrier between tbe applicator and the
 * application is on the __tc_managed().getResolveLock().
 */
public class HashtableTC extends Hashtable implements TCMap, Manageable, Clearable {

  private volatile transient TCObject $__tc_MANAGED;
  private boolean                     evictionEnabled = true;

  @Override
  public synchronized void clear() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        ManagerUtil.logicalInvoke(this, SerializationUtil.CLEAR_SIGNATURE, new Object[0]);
        super.clear();
      }
    } else {
      super.clear();
    }
  }

  @Override
  public synchronized Object clone() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        Hashtable clone = (Hashtable) super.clone();

        for (Iterator it = clone.entrySet().iterator(); it.hasNext();) {
          Map.Entry cloneEntry = (Map.Entry) it.next();
          // make sure any cleared references are looked-up before returning the clone
          // otherwise the clone may end up having ValueWrapper's with ObjectID's instead of the actual value object
          ((HashtableTC) clone).lookUpAndStoreIfNecessary(cloneEntry);
        }
        return CloneUtil.fixTCObjectReferenceOfClonedObject(this, clone);
      }
    }

    return super.clone();
  }

  // Values that contains ObjectIDs are already wrapped, so this should be fine
  @Override
  public synchronized boolean contains(Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.contains(value);
      }
    } else {
      return super.contains(value);
    }
  }

  // XXX:: Keys can't be ObjectIDs as of Now.
  @Override
  public synchronized boolean containsKey(Object key) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.containsKey(key);
      }
    } else {
      return super.containsKey(key);
    }
  }

  @Override
  public boolean containsValue(Object value) {
    // super.containsValue() simply calls contains, which is already synchronize on Hashtable and getResolveLock().
    return super.containsValue(value);
  }

  @Override
  public synchronized boolean equals(Object o) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.equals(o);
      }
    }
    return super.equals(o);
  }

  /*
   * This method uses __tc_getEntry() instead of a get() and put() to avoid changing the modCount in shared mode
   */
  @Override
  public synchronized Object get(Object key) {
    if (__tc_isManaged()) {
      Map.Entry e = null;
      synchronized (__tc_managed().getResolveLock()) {
        e = __tc_getEntry(key);
      }
      if (e == null) return null;
      Object actualValue = lookUpAndStoreIfNecessary(e);
      return actualValue;
    } else {
      return super.get(key);
    }
  }

  private Object lookUpAndStoreIfNecessary(Map.Entry e) {
    Object value = null;
    synchronized (__tc_managed().getResolveLock()) {
      value = e.getValue();
    }
    Object actualValue = unwrapValueIfNecessary(value);
    storeValueIfValid(e, actualValue);
    return actualValue;
  }

  private void storeValueIfValid(Map.Entry preLookupEntry, Object resolvedValue) {
    synchronized (__tc_managed().getResolveLock()) {
      Map.Entry postLookupEntry = __tc_getEntry(preLookupEntry.getKey());
      if (postLookupEntry != null && preLookupEntry.getValue() == postLookupEntry.getValue()
          && resolvedValue != preLookupEntry.getValue()) {
        preLookupEntry.setValue(resolvedValue);
      }
    }
  }

  @Override
  public synchronized int hashCode() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.hashCode();
      }
    }
    return super.hashCode();
  }

  @Override
  public synchronized boolean isEmpty() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.isEmpty();
      }
    } else {
      return super.isEmpty();
    }
  }

  /*
   * This method needs to call logicalInvoke before modifying the local state to avoid inconsistency when throwing
   * NonPortableExceptions.
   */
  @Override
  public synchronized Object put(Object key, Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        if (key == null || value == null) { throw new NullPointerException(); }
        ManagerUtil.checkWriteAccess(this);
        Entry e = __tc_getEntry(key);
        if (e == null) {
          // New mapping
          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
          // Sucks to do a second lookup !!
          return unwrapValueIfNecessary(super.put(key, wrapValueIfNecessary(value)));
        } else {
          Object old = unwrapValueIfNecessary(e.getValue());
          if (old != value) {
            ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { e.getKey(), value });
            e.setValue(wrapValueIfNecessary(value));
          }
          return old;
        }
      }
    } else {
      return super.put(key, value);
    }
  }

  /**
   * This method is only to be invoked from the applicator thread. This method does not need to check if the map is
   * managed as it will always be managed when called by the applicator thread. In addition, this method does not need
   * to be synchronized under getResolveLock() as the applicator thread is already under the scope of such
   * synchronization. This method does not need to be synchronized on this either as both the application and the
   * applicator will be synchronized on the getResolveLock().
   */
  public void __tc_applicator_put(Object key, Object value) {
    if (key == null || value == null) { throw new NullPointerException(); }
    super.put(key, wrapValueIfNecessary(value));
  }

  private static Object unwrapValueIfNecessary(Object value) {
    if (value instanceof ValuesWrapper) {
      return ((ValuesWrapper) value).getValue();
    } else {
      return value;
    }
  }

  private static Object unwrapValueIfNecessaryFaultBreadth(Object value, ObjectID parentContext) {
    if (value instanceof ValuesWrapper) {
      return ((ValuesWrapper) value).getValueFaultBreadth(parentContext);
    } else {
      return value;
    }
  }

  private static Object wrapValueIfNecessary(Object value) {
    if (value instanceof ObjectID) {
      // value cant be NULL_ID as Hashtable doesnt handle null !
      return new ValuesWrapper(value);
    } else {
      return value;
    }
  }

  @Override
  public synchronized void putAll(Map t) {
    if (__tc_isManaged()) {
      if (t.isEmpty()) return;

      Iterator i = t.entrySet().iterator();
      while (i.hasNext()) {
        Map.Entry e = (Map.Entry) i.next();
        __tc_put_logical(e.getKey(), e.getValue());
      }
    } else {
      super.putAll(t);
    }
  }

  @Override
  public synchronized Object remove(Object key) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);

        Entry entry = __tc_removeEntryForKey(key);
        if (entry == null) { return null; }

        Object rv = unwrapValueIfNecessary(entry.getValue());

        ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { entry.getKey() });

        return rv;
      }
    } else {
      return super.remove(key);
    }
  }

  /**
   * This method is only to be invoked from the applicator thread. This method does not need to check if the map is
   * managed as it will always be managed when called by the applicator thread. In addition, this method does not need
   * to be synchronized under getResolveLock() as the applicator thread is already under the scope of such
   * synchronization.
   */
  public void __tc_applicator_remove(Object key) {
    super.remove(key);
  }

  /**
   * This method is only to be invoked from the applicator thread. This method does not need to check if the map is
   * managed as it will always be managed when called by the applicator thread. In addition, this method does not need
   * to be synchronized under getResolveLock() as the applicator thread is already under the scope of such
   * synchronization.
   */
  public void __tc_applicator_clear() {
    super.clear();
  }

  /**
   * This method is to be invoked when one needs a remove to get broadcast, but do not want to fault in the value of a
   * map entry.
   */
  public synchronized void __tc_remove_logical(Object key) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);

        Entry entry = __tc_removeEntryForKey(key);
        if (entry == null) { return; }

        ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { entry.getKey() });
      }
    } else {
      super.remove(key);
    }
  }

  /**
   * This method is to be invoked when one needs a put to get broadcast, but do not want to fault in the value of a map
   * entry.
   */
  public synchronized void __tc_put_logical(Object key, Object value) {
    if (__tc_isManaged()) {
      if (key == null || value == null) { throw new NullPointerException(); }
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        Entry e = __tc_getEntry(key);
        if (e == null) {
          // New mapping
          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
          // Sucks to do a second lookup !!
          super.put(key, wrapValueIfNecessary(value));
        } else {
          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { e.getKey(), value });
          e.setValue(wrapValueIfNecessary(value));
        }
      }
    } else {
      super.put(key, value);
    }
  }

  public synchronized Collection __tc_getAllEntriesSnapshot() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return __tc_getAllEntriesSnapshotInternal();
      }
    } else {
      return __tc_getAllEntriesSnapshotInternal();
    }
  }

  private Collection __tc_getAllEntriesSnapshotInternal() {
    Set entrySet = super.entrySet();
    return new ArrayList(entrySet);
  }

  public synchronized Collection __tc_getAllLocalEntriesSnapshot() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return __tc_getAllLocalEntriesSnapshotInternal();
      }
    } else {
      return __tc_getAllLocalEntriesSnapshotInternal();
    }
  }

  private Collection __tc_getAllLocalEntriesSnapshotInternal() {
    Set entrySet = super.entrySet();
    int entrySetSize = entrySet.size();
    if (entrySetSize == 0) { return Collections.EMPTY_LIST; }

    Object[] tmp = new Object[entrySetSize];
    int index = -1;
    for (Iterator i = entrySet.iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      if (!(e.getValue() instanceof ValuesWrapper)) {
        index++;
        tmp[index] = e;
      }
    }

    if (index < 0) { return Collections.EMPTY_LIST; }
    Object[] rv = new Object[index + 1];
    System.arraycopy(tmp, 0, rv, 0, index + 1);
    return Arrays.asList(rv);
  }

  @Override
  public synchronized int size() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return super.size();
      }
    } else {
      return super.size();
    }
  }

  @Override
  public synchronized String toString() {
    return super.toString();
  }

  @Override
  public synchronized Enumeration keys() {
    // XXX: when/if partial keys are implemented this method will need to behave differently
    return super.keys();
  }

  @Override
  public Set keySet() {
    Collections.SynchronizedSet ss = (SynchronizedSet) super.keySet();
    return new KeySetWrapper((Set) ss.c);
  }

  @Override
  public synchronized Enumeration elements() {
    // note: always returning a wrapper since this view can be created on a unshared Hashtable that can later become
    // shared
    return new ValueUnwrappingEnumeration(super.elements());
  }

  @Override
  public Set entrySet() {
    return nonOverridableEntrySet();
  }

  private Set nonOverridableEntrySet() {
    Collections.SynchronizedSet ss = (SynchronizedSet) super.entrySet();
    return new EntrySetWrapper((Set) ss.c);
  }

  @Override
  public Collection values() {
    Collections.SynchronizedCollection sc = (SynchronizedCollection) super.values();
    return new ValuesCollectionWrapper(sc.c);
  }

  /**
   * Clearable interface - called by CacheManager thru TCObjectLogical
   */
  public int __tc_clearReferences(int toClear) {
    if (!__tc_isManaged()) { throw new AssertionError("clearReferences() called on Unmanaged Map"); }
    synchronized (__tc_managed().getResolveLock()) {
      int cleared = 0;
      for (Iterator i = super.entrySet().iterator(); i.hasNext() && toClear > cleared;) {
        Map.Entry e = (Map.Entry) i.next();

        TCObjectExternal tcObject = ManagerUtil.lookupExistingOrNull(e.getValue());
        if (tcObject != null && !tcObject.recentlyAccessed()) {
          ObjectID oid = tcObject.getObjectID();
          e.setValue(wrapValueIfNecessary(oid));
          cleared++;
        }

      }
      return cleared;
    }
  }

  public boolean isEvictionEnabled() {
    return evictionEnabled;
  }

  public void setEvictionEnabled(boolean enabled) {
    evictionEnabled = enabled;
  }

  public void __tc_managed(TCObject tcObject) {
    $__tc_MANAGED = tcObject;
  }

  public TCObject __tc_managed() {
    return $__tc_MANAGED;
  }

  public boolean __tc_isManaged() {
    // TCObject tcManaged = $__tc_MANAGED;
    // return (tcManaged != null && (tcManaged instanceof TCObjectPhysical || tcManaged instanceof TCObjectLogical));
    return $__tc_MANAGED != null;
  }

  protected Map.Entry __tc_getEntry(Object key) {
    // This method is instrumented during bootjar creation into the vanilla (which gets tainted) java.util.Hashtable.
    // This is needed so that we can easily get access to the Original Key on put without a traversal or proxy Keys.
    throw new RuntimeException("This should never execute! Check BootJarTool");
  }

  protected Map.Entry __tc_removeEntryForKey(Object key) {
    // This method is instrumented during bootjar creation into the vanilla (which gets tainted) java.util.Hashtable.
    throw new RuntimeException("This should never execute! Check BootJarTool");
  }

  private static class ValuesWrapper {

    private Object value;

    public ValuesWrapper(Object value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      return getValue().equals(obj);
    }

    Object getValue() {
      if (value instanceof ObjectID) {
        try {
          value = ManagerUtil.lookupObject((ObjectID) value);
        } catch (TCObjectNotFoundException onfe) {
          throw new ConcurrentModificationException(onfe.getMessage());
        }
      }
      return value;
    }

    public Object getValueFaultBreadth(ObjectID parentContext) {
      if (value instanceof ObjectID) {
        try {
          value = ManagerUtil.lookupObjectWithParentContext((ObjectID) value, parentContext);
        } catch (TCObjectNotFoundException onfe) {
          throw new ConcurrentModificationException(onfe.getMessage());
        }
      }
      return value;
    }

    @Override
    public int hashCode() {
      return getValue().hashCode();
    }

    @Override
    public String toString() {
      return getValue().toString();
    }
  }

  /**
   * Methods of this class do not need to be synchronized on Hashtable instance when it is not shared since methods of
   * Hashtable.Entry is not synchronized. When it is shared, we need to synchronize on getResolveLock for read operation
   * to maintain a common barrier with the applicator thread. For mutate operation setValue, we need to synchronize on
   * the Hashtable also to create a transaction.
   */
  private class EntryWrapper implements Map.Entry {

    private final Entry entry;

    public EntryWrapper(Entry entry) {
      this.entry = entry;
    }

    @Override
    public boolean equals(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return entry.equals(o);
        }
      } else {
        return entry.equals(o);
      }
    }

    public Object getKey() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return entry.getKey();
        }
      } else {
        return entry.getKey();
      }
    }

    public Object getValue() {
      if (__tc_isManaged()) {
        synchronized (HashtableTC.this) {
          return lookUpAndStoreIfNecessary(entry);
        }
      } else {
        return entry.getValue();
      }
    }

    private Object getEntryValue() {
      synchronized (__tc_managed().getResolveLock()) {
        return entry.getValue();
      }
    }

    public Object getValueFaultBreadth() {
      if (__tc_isManaged()) {
        synchronized (HashtableTC.this) {
          Object preLookupValue = getEntryValue();
          Object value = unwrapValueIfNecessaryFaultBreadth(preLookupValue, __tc_managed().getObjectID());
          synchronized (__tc_managed().getResolveLock()) {
            Object postLookupValue = entry.getValue();
            if (postLookupValue != value && postLookupValue == preLookupValue) {
              entry.setValue(value);
            }
            return value;
          }
        }
      } else {
        return entry.getValue();
      }
    }

    @Override
    public int hashCode() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return entry.hashCode();
        }
      } else {
        return entry.hashCode();
      }
    }

    public Object setValue(Object value) {
      if (__tc_isManaged()) {
        synchronized (HashtableTC.this) {
          synchronized (__tc_managed().getResolveLock()) {
            // This check is done to solve the chicken and egg problem. Should I modify the local copy or the remote
            // copy
            // ? (both has error checks that we want to take place before any modification is propagated
            if (value == null) throw new NullPointerException();
            ManagerUtil.checkWriteAccess(HashtableTC.this);
            ManagerUtil.logicalInvoke(HashtableTC.this, SerializationUtil.PUT_SIGNATURE,
                                      new Object[] { getKey(), value });
            return unwrapValueIfNecessary(entry.setValue(value));
          }
        }
      } else {
        return entry.setValue(value);
      }
    }
  }

  private abstract class CollectionWrapper extends AbstractCollection implements Serializable {
    // This default constructor is added so that the compile will not generate a HashtableTC$1 dummy class. No idea why
    // the compiler did that.
    public CollectionWrapper() {
      super();
    }

    @Override
    public Object[] toArray() {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.toArray();
          }
        } else {
          return super.toArray();
        }
      }
    }

    @Override
    public Object[] toArray(Object[] a) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.toArray(a);
          }
        } else {
          return super.toArray(a);
        }
      }
    }

    @Override
    public boolean containsAll(Collection col) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.containsAll(col);
          }
        } else {
          return super.containsAll(col);
        }
      }
    }

    @Override
    public boolean removeAll(Collection col) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.removeAll(col);
          }
        } else {
          return super.removeAll(col);
        }
      }
    }

    @Override
    public boolean retainAll(Collection col) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.retainAll(col);
          }
        } else {
          return super.retainAll(col);
        }
      }
    }

    @Override
    public String toString() {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return super.toString();
          }
        } else {
          return super.toString();
        }
      }
    }

    private synchronized void writeObject(ObjectOutputStream s) throws IOException {
      s.defaultWriteObject();
    }
  }

  private class EntrySetWrapper extends CollectionWrapper implements Set {

    private final Set entrySet;

    public EntrySetWrapper(Set entrySet) {
      this.entrySet = entrySet;
    }

    @Override
    public boolean add(Object arg0) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return entrySet.add(arg0);
          }
        } else {
          return entrySet.add(arg0);
        }
      }
    }

    @Override
    public void clear() {
      // XXX:: Calls Hashtable.clear()
      entrySet.clear();
    }

    @Override
    public boolean contains(Object o) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            return entrySet.contains(o);
          }
        } else {
          return entrySet.contains(o);
        }
      }
    }

    @Override
    public Iterator iterator() {
      return new EntriesIterator(entrySet.iterator());
    }

    @Override
    public boolean remove(Object o) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          synchronized (__tc_managed().getResolveLock()) {
            ManagerUtil.checkWriteAccess(HashtableTC.this);

            if (!(o instanceof Map.Entry)) { return false; }

            Entry entryToRemove = (Entry) o;

            if (entrySet.contains(entryToRemove)) {
              Entry entry = __tc_removeEntryForKey(entryToRemove.getKey());
              ManagerUtil.logicalInvoke(HashtableTC.this, SerializationUtil.REMOVE_KEY_SIGNATURE, new Object[] { entry
                  .getKey() });
              return true;
            } else {
              return false;
            }
          }
        } else {
          return entrySet.remove(o);
        }
      }
    }

    @Override
    public int size() {
      return HashtableTC.this.size();
    }
  }

  private class KeySetWrapper extends CollectionWrapper implements Set {
    private final Set keys;

    public KeySetWrapper(Set keys) {
      this.keys = keys;
    }

    @Override
    public void clear() {
      // Calls Hashtable.this.clear().
      keys.clear();
    }

    @Override
    public boolean contains(Object o) {
      // Calls Hashtable.this.containsKey().
      return keys.contains(o);
    }

    @Override
    public Iterator iterator() {
      return new KeysIterator(nonOverridableEntrySet().iterator());
    }

    // XXX:: Calls Hashtable.remove();
    @Override
    public boolean remove(Object o) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          // Managed version
          int sizeB4 = size();
          HashtableTC.this.__tc_remove_logical(o);
          return (size() != sizeB4);
        } else {
          return keys.remove(o);
        }
      }
    }

    @Override
    public boolean removeAll(Collection c) {
      synchronized (HashtableTC.this) {
        if (__tc_isManaged()) {
          boolean modified = false;

          synchronized (__tc_managed().getResolveLock()) {
            ManagerUtil.checkWriteAccess(HashtableTC.this);

            if (size() > c.size()) {
              for (Iterator i = c.iterator(); i.hasNext();) {
                Entry entry = __tc_removeEntryForKey(i.next());
                if (entry != null) {
                  ManagerUtil.logicalInvoke(HashtableTC.this, SerializationUtil.REMOVE_KEY_SIGNATURE,
                                            new Object[] { entry.getKey() });
                  modified = true;
                }
              }
            } else {
              for (Iterator i = iterator(); i.hasNext();) {
                if (c.contains(i.next())) {
                  i.remove();
                  modified = true;
                }
              }
            }
          }
          return modified;
        } else {
          return super.removeAll(c);
        }
      }
    }

    @Override
    public int size() {
      return HashtableTC.this.size();
    }

  }

  private class ValuesCollectionWrapper extends CollectionWrapper {

    private final Collection values;

    public ValuesCollectionWrapper(Collection values) {
      this.values = values;
    }

    // XXX:: Calls Hashtable.this.clear();
    @Override
    public void clear() {
      values.clear();
    }

    // XXX:: Calls Hashtable.this.containsValue();
    @Override
    public boolean contains(Object o) {
      return values.contains(o);
    }

    @Override
    public Iterator iterator() {
      return new ValuesIterator(nonOverridableEntrySet().iterator());
    }

    @Override
    public int size() {
      return HashtableTC.this.size();
    }
  }

  // Hashtable Iterator does not synchronize access to the table. We synchronize on the getResolveLock() to have a
  // common barrier with the applicator.
  private class EntriesIterator implements Iterator {

    private final Iterator entries;
    private Map.Entry      currentEntry;

    public EntriesIterator(Iterator entries) {
      this.entries = entries;
    }

    public boolean hasNext() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return entries.hasNext();
        }
      } else {
        return entries.hasNext();
      }
    }

    public Object next() {
      currentEntry = nextEntry();
      if (currentEntry instanceof EntryWrapper) {
        // This check is here since this class is extended by ValuesIterator too.
        return currentEntry;
      } else {
        return new EntryWrapper(currentEntry);
      }
    }

    protected Map.Entry nextEntry() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return (Map.Entry) entries.next();
        }
      } else {
        return (Map.Entry) entries.next();
      }
    }

    public void remove() {
      if (__tc_isManaged()) {
        // We need to synchronize on Hashtable here to create a transaction
        synchronized (HashtableTC.this) {
          synchronized (__tc_managed().getResolveLock()) {
            ManagerUtil.checkWriteAccess(HashtableTC.this);
            entries.remove();
            ManagerUtil.logicalInvoke(HashtableTC.this, SerializationUtil.REMOVE_KEY_SIGNATURE,
                                      new Object[] { currentEntry.getKey() });
          }
        }
      } else {
        entries.remove();
      }
    }
  }

  private class KeysIterator extends EntriesIterator {

    public KeysIterator(Iterator entries) {
      super(entries);
    }

    @Override
    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      return e.getKey();
    }
  }

  private class ValuesIterator extends EntriesIterator {

    public ValuesIterator(Iterator entries) {
      super(entries);
    }

    @Override
    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      if (e instanceof EntryWrapper) {
        EntryWrapper ew = (EntryWrapper) e;
        return ew.getValueFaultBreadth();
      }
      return e.getValue();
    }
  }

  private class ValueUnwrappingEnumeration implements Enumeration {

    private final Enumeration e;

    public ValueUnwrappingEnumeration(Enumeration e) {
      this.e = e;
    }

    public boolean hasMoreElements() {
      return e.hasMoreElements();
    }

    public Object nextElement() {
      Object rv = e.nextElement();
      if (rv instanceof ValuesWrapper) {
        rv = ((ValuesWrapper) rv).getValue();
      }
      return rv;
    }
  }
}
