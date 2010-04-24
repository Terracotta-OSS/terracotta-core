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
import com.tc.object.bytecode.HashMapClassAdapter;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.util.Assert;

/*
 * This class will be merged with java.lang.HashMap in the bootjar. This HashMap can store ObjectIDs instead of Objects
 * to save memory and transparently fault Objects as needed. It can also clear references. After merging, the {@link
 * HashMapClassAdapter} will be applied, which will instrument either the entrySet0 method (if present) or the entrySet
 * method otherwise.
 */
public class HashMapTC extends HashMap implements TCMap, Manageable, Clearable {

  // General Rules to follow in this class
  // 1) Values could be ObjectIDs. In shared mode, always do a lookup before returning to outside world.
  // 2) If you do a lookup, try to store back the actual object into the Map if the key is available.
  // 3) Be careful about existing iterators. It shouldn't throw exception because of (2)
  // 4) When you do so, call markAccessed() to make the cache eviction correct
  // 5) Check write access before any shared changed
  // 6) Call logical Invoke before changing internal state so that NonPortableExceptions don't modify state.
  //

  // TODO:: markAccessed()
  private volatile transient TCObject $__tc_MANAGED;
  private boolean                     evictionEnabled = true;

  @Override
  public void clear() {
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
  public boolean containsKey(Object key) {
    // XXX:: Keys can't be ObjectIDs as of Now.
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        // Just to have proper memory boundary
        return super.containsKey(key);
      }
    } else {
      return super.containsKey(key);
    }
  }

  /*
   * This method is overridden in LinkedHashMap. so any change here needs to be propagated to LinkedHashMap too.
   */
  @Override
  public boolean containsValue(Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        if (value != null) {
          // XXX:: This is tied closely to HashMap implementation which calls equals on the passed value rather than the
          // other way around
          return super.containsValue(new ValueWrapper(value));
        } else {
          // It is a little weird to do this like this, o well...
          return super.containsValue(value) || super.containsValue(ObjectID.NULL_ID);
        }
      }
    } else {
      return super.containsValue(value);
    }
  }

  // XXX: This uses entrySet iterator and since we fix that, this should work.
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }

  /*
   * This method is overridden in LinkedHashMapTC. so any change here needs to be propagated to LinkedHashMapTC too.
   * XXX:: This method uses getEntry instead of a get and put to avoid changing the modCount in shared mode
   */
  @Override
  public Object get(Object key) {
    if (__tc_isManaged()) {
      Map.Entry e = __tc_getEntryUnderResolvedLock(key);
      return lookUpAndStoreIfNecessary(e);
    } else {
      return super.get(key);
    }
  }

  private Object lookUpAndStoreIfNecessary(Map.Entry e) {
    if (e == null) return null;
    Object value = null;
    synchronized (__tc_managed().getResolveLock()) {
      value = e.getValue();
    }
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

  private Map.Entry __tc_getEntryUnderResolvedLock(Object key) {
    synchronized (__tc_managed().getResolveLock()) {
      return getEntry(key);
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

  private Object lookUpFaultBreadthIfNecessary(Object o) {
    if (o instanceof ObjectID) {
      try {
        return ManagerUtil.lookupObjectWithParentContext((ObjectID) o, __tc_managed().getObjectID());
      } catch (TCObjectNotFoundException onfe) {
        throw new ConcurrentModificationException(onfe.getMessage());
      }
    }
    return o;
  }

  // XXX: This uses entrySet iterator and since we fix that, this should work.
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean isEmpty() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        // Just to have proper memory boundary
        return super.isEmpty();
      }
    } else {
      return super.isEmpty();
    }
  }

  @Override
  public void putAll(Map map) {
    if (__tc_isManaged()) {
      int numKeysToBeAdded = map.size();
      if (numKeysToBeAdded == 0) return;

      /*
       * This logic duplicated from HashMap super implementation see explanation there
       */
      if (numKeysToBeAdded > threshold) {
        int targetCapacity = (int) (numKeysToBeAdded / loadFactor + 1);
        if (targetCapacity > MAXIMUM_CAPACITY) targetCapacity = MAXIMUM_CAPACITY;
        int newCapacity = table.length;
        while (newCapacity < targetCapacity)
          newCapacity <<= 1;
        if (newCapacity > table.length) resize(newCapacity);
      }

      for (Iterator i = map.entrySet().iterator(); i.hasNext();) {
        Map.Entry e = (Map.Entry) i.next();
        __tc_put_logical(e.getKey(), e.getValue());
      }
    } else {
      super.putAll(map);
    }
  }

  @Override
  public Object remove(Object key) {
    if (__tc_isManaged()) {
      // Managed Version
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);

        Entry entry = removeEntryForKey(key);
        if (entry == null) { return null; }

        ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_ENTRY_FOR_KEY_SIGNATURE,
                                  new Object[] { entry.getKey() });

        return lookUpIfNecessary(entry.getValue());
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
  public void __tc_remove_logical(Object key) {
    if (__tc_isManaged()) {
      // Managed Version
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);

        Entry entry = removeEntryForKey(key);
        if (entry == null) { return; }

        ManagerUtil.logicalInvoke(this, SerializationUtil.REMOVE_ENTRY_FOR_KEY_SIGNATURE,
                                  new Object[] { entry.getKey() });

        return;
      }
    } else {
      super.remove(key);
    }
  }

  /**
   * This method is to be invoked when one needs a put to get broadcast, but do not want to fault in the value of a map
   * entry.
   */
  public void __tc_put_logical(Object key, Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        // It sucks todo two lookups
        HashMap.Entry e = getEntry(key);
        if (e == null) {
          // New mapping
          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
          super.put(key, value);
        } else {
          // without this, LinkedHashMap will not function properly
          e.recordAccess(this);

          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { e.getKey(), value });
          e.setValue(value);
        }
      }
    } else {
      super.put(key, value);
    }
  }

  public Collection __tc_getAllEntriesSnapshot() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return __tc_getAllEntriesSnapshotInternal();
      }
    } else {
      return super.entrySet();
    }
  }

  private Collection __tc_getAllEntriesSnapshotInternal() {
    EntrySetWrapper entrySet = (EntrySetWrapper) super.entrySet();
    return new ArrayList(entrySet.__tc_getLocalEntries());
  }

  public Collection __tc_getAllLocalEntriesSnapshot() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        return __tc_getAllLocalEntriesSnapshotInternal();
      }
    } else {
      return super.entrySet();
    }
  }

  private Collection __tc_getAllLocalEntriesSnapshotInternal() {
    EntrySetWrapper entrySet = (EntrySetWrapper) super.entrySet();
    int entrySetSize = entrySet.__tc_getLocalEntriesSize();
    if (entrySetSize == 0) { return Collections.EMPTY_LIST; }

    Object[] tmp = new Object[entrySetSize];
    int index = -1;
    for (Iterator i = entrySet.iterator(); i.hasNext();) {
      EntryWrapper e = (EntryWrapper) i.next();
      if (!(e.__tc_getLocalValue() instanceof ObjectID)) {
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
  public Object clone() {
    Manageable clone = (Manageable) super.clone();
    return CloneUtil.fixTCObjectReferenceOfClonedObject(this, clone);
  }

  /*
   * This method needs to call logicalInvoke before modifying the local state to avoid inconsistency when throwing
   * NonPortableExceptions TODO:: provide special method for the applicator
   */
  @Override
  public Object put(Object key, Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        // It sucks todo two lookups
        HashMap.Entry e = getEntry(key);
        if (e == null) {
          // New mapping
          ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { key, value });
          return lookUpIfNecessary(super.put(key, value));
        } else {
          // without this, LinkedHashMap will not function properly
          e.recordAccess(this);

          // Replacing old mapping
          Object old = lookUpIfNecessary(e.getValue());
          if (value != old) {
            ManagerUtil.logicalInvoke(this, SerializationUtil.PUT_SIGNATURE, new Object[] { e.getKey(), value });
            e.setValue(value);
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
   * synchronization.
   */
  public void __tc_applicator_put(Object key, Object value) {
    super.put(key, value);
  }

  @Override
  public int size() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        // Just to have proper memory boundary
        return super.size();
      }
    } else {
      return super.size();
    }
  }

  @Override
  public String toString() {
    return super.toString();
  }

  @Override
  public Set keySet() {
    return new KeySetWrapper(super.keySet());
  }

  @Override
  public Collection values() {
    return new ValuesCollectionWrapper(super.values());
  }

  @Override
  public Set entrySet() {
    return this.nonOverridableEntrySet();
  }

  private final Set nonOverridableEntrySet() {
    return super.entrySet();
  }

  /**
   * Clearable interface - called by CacheManager thru TCObjectLogical
   */
  public int __tc_clearReferences(int toClear) {
    if (!__tc_isManaged()) { throw new AssertionError("clearReferences() called on Unmanaged Map"); }
    synchronized (__tc_managed().getResolveLock()) {
      int cleared = 0;
      for (Iterator i = super.entrySet().iterator(); i.hasNext() && toClear > cleared;) {
        EntryWrapper e = (EntryWrapper) i.next();

        TCObjectExternal tcObject = ManagerUtil.lookupExistingOrNull(e.__tc_getLocalValue());
        if (tcObject != null && !tcObject.recentlyAccessed()) {
          ObjectID oid = tcObject.getObjectID();
          e.__tc_setLocalValue(oid);
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

  /*
   * This wrapper depends on the fact that key.equals() gets called on the wrapper instead of the other way around
   */
  static class ValueWrapper {

    private final Object value;

    public ValueWrapper(Object value) {
      this.value = value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      Object pojo = lookUpIfNecessary(o); // XXX:: This is not stored in the Map since we dont know the key
      return pojo == value || value.equals(pojo);
    }
  }

  private class EntryWrapper implements Map.Entry {

    private final Map.Entry entry;

    public EntryWrapper(Map.Entry entry) {
      Assert.assertFalse(entry instanceof EntryWrapper);
      this.entry = entry;
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

    // XXX:: This method has the side effect of looking up the object and setting the value in the Managed case.
    // This method not only does a faulting on this value, but depending on the fault depth, it faults peer objects too.
    public Object getValue() {
      if (__tc_isManaged()) {
        Object preLookupValue = lookUpAndStoreIfNecessary(entry);
        Object value = lookUpFaultBreadthIfNecessary(preLookupValue);
        synchronized (__tc_managed().getResolveLock()) {
          Object postLookupValue = entry.getValue();
          if (postLookupValue != value && postLookupValue == preLookupValue) {
            entry.setValue(value);
          }
          return value;
        }
      } else {
        return entry.getValue();
      }
    }

    private Object __tc_getLocalValue() {
      return entry.getValue();
    }

    private Object __tc_setLocalValue(Object value) {
      return entry.setValue(value);
    }

    /*
     * Even though we do a lookup of oldVal after we change the value in the transaction, DGC will not be able to kick
     * the oldVal out since the transaction is not committed.
     */
    public Object setValue(Object value) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          ManagerUtil.checkWriteAccess(HashMapTC.this);
          ManagerUtil.logicalInvoke(HashMapTC.this, SerializationUtil.PUT_SIGNATURE, new Object[] { entry.getKey(),
              value });
          Object oldVal = entry.setValue(value);
          return lookUpIfNecessary(oldVal);
        }
      } else {
        return entry.setValue(value);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          // XXX:: make sure value is lookedup
          getValue();
          return entry.equals(o);
        }
      } else {
        return entry.equals(o);
      }
    }

    @Override
    public int hashCode() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          // XXX:: make sure value is lookedup
          getValue();
          return entry.hashCode();
        }
      } else {
        return entry.hashCode();
      }
    }

  }

  /**
   * This inner class is used by {@link HashMapClassAdapter}
   */
  class EntrySetWrapper extends AbstractSet {

    private final Set entries;

    public EntrySetWrapper(Set entries) {
      this.entries = entries;
    }

    @Override
    public void clear() {
      HashMapTC.this.clear();
    }

    // Has to take care of ObjectIDs
    @Override
    public boolean contains(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          if (!(o instanceof Map.Entry)) return false;
          Map.Entry e = (Map.Entry) o;
          Object key = e.getKey();
          if (!HashMapTC.this.containsKey(key)) { return false; }

          Map.Entry candidate = HashMapTC.this.getEntry(key);
          lookUpAndStoreIfNecessary(candidate);
          return candidate.equals(e);
        }
      } else {
        return entries.contains(o);
      }
    }

    @Override
    public Iterator iterator() {
      return new UnwrappedEntriesIterator(entries.iterator());
    }

    @Override
    public boolean remove(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          if (!(o instanceof Map.Entry)) return false;
          Map.Entry e = (Map.Entry) o;
          if (contains(e)) {
            int sizeB4 = size();
            HashMapTC.this.remove(e.getKey());
            return (sizeB4 != size());
          } else {
            return false;
          }
        }
      } else {
        return entries.remove(o);
      }
    }

    private int __tc_getLocalEntriesSize() {
      return this.entries.size();
    }

    private Set __tc_getLocalEntries() {
      return this.entries;
    }

    @Override
    public int size() {
      return HashMapTC.this.size();
    }

  }

  // These wrapper object are needed only for giving proper memory boundary to size() calls.
  private class KeySetWrapper extends AbstractSet {

    private final Set _keySet;

    public KeySetWrapper(Set keySet) {
      this._keySet = keySet;
    }

    @Override
    public void clear() {
      HashMapTC.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return _keySet.contains(o);
        }
      } else {
        return _keySet.contains(o);
      }
    }

    @Override
    public Iterator iterator() {
      return new KeysIterator(HashMapTC.this.nonOverridableEntrySet().iterator());
    }

    @Override
    public boolean remove(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          // Managed version
          int sizeB4 = size();
          HashMapTC.this.__tc_remove_logical(o);
          return (size() != sizeB4);
        }
      } else {
        return _keySet.remove(o);
      }
    }

    @Override
    public boolean removeAll(Collection c) {
      if (__tc_isManaged()) {
        boolean modified = false;
        ManagerUtil.checkWriteAccess(HashMapTC.this);

        if (size() > c.size()) {
          for (Iterator i = c.iterator(); i.hasNext();) {
            Entry entry = removeEntryForKey(i.next());
            if (entry != null) {
              ManagerUtil.logicalInvoke(HashMapTC.this, SerializationUtil.REMOVE_ENTRY_FOR_KEY_SIGNATURE,
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
        return modified;
      } else {
        return _keySet.removeAll(c);
      }
    }

    @Override
    public int size() {
      return HashMapTC.this.size();
    }

  }

  private class ValuesCollectionWrapper extends AbstractCollection {

    private final Collection _values;

    public ValuesCollectionWrapper(Collection values) {
      this._values = values;
    }

    @Override
    public void clear() {
      HashMapTC.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          // Managed version
          if (o != null) {
            return _values.contains(new ValueWrapper(o));
          } else {
            return _values.contains(o);
          }
        }
      } else {
        return _values.contains(o);
      }
    }

    @Override
    public Iterator iterator() {
      return new ValuesIterator(HashMapTC.this.nonOverridableEntrySet().iterator());
    }

    @Override
    public int size() {
      return HashMapTC.this.size();
    }

  }

  private abstract class AbstractManagedEntriesIterator implements Iterator {

    private final Iterator iterator;
    private Map.Entry      currentEntry;

    public AbstractManagedEntriesIterator(Iterator iterator) {
      this.iterator = iterator;
    }

    public boolean hasNext() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return iterator.hasNext();
        }
      } else {
        return iterator.hasNext();
      }
    }

    public Object next() {
      return currentEntry = nextEntry();
    }

    private Map.Entry nextEntry() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return postNextEntry((Map.Entry) iterator.next());
        }
      } else {
        return (Map.Entry) iterator.next();
      }
    }

    /* overridable if necessary */
    protected Map.Entry postNextEntry(Map.Entry entry) {
      return entry;
    }

    public void remove() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          ManagerUtil.checkWriteAccess(HashMapTC.this);
          iterator.remove();
          ManagerUtil.logicalInvoke(HashMapTC.this, SerializationUtil.REMOVE_ENTRY_FOR_KEY_SIGNATURE,
                                    new Object[] { currentEntry.getKey() });
        }
      } else {
        iterator.remove();
      }
    }
  }

  private class UnwrappedEntriesIterator extends AbstractManagedEntriesIterator {

    public UnwrappedEntriesIterator(Iterator iterator) {
      super(iterator);
    }

    @Override
    protected Map.Entry postNextEntry(Map.Entry entry) {
      return new EntryWrapper(entry);
    }
  }

  private class KeysIterator extends AbstractManagedEntriesIterator {

    public KeysIterator(Iterator iterator) {
      super(iterator);
    }

    @Override
    public Object next() {
      return ((Map.Entry) super.next()).getKey();
    }
  }

  private class ValuesIterator extends AbstractManagedEntriesIterator {

    public ValuesIterator(Iterator iterator) {
      super(iterator);
    }

    @Override
    public Object next() {
      return ((Map.Entry) super.next()).getValue();
    }
  }

}
