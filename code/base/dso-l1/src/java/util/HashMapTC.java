/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package java.util;

import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.hook.impl.Util;

/*
 * This class will be merged with java.lang.HashMap in the bootjar. This HashMap can store ObjectIDs instead of Objects
 * to save memory and transparently fault Objects as needed. It can also clear references.
 */
public class HashMapTC extends HashMap implements TCMap, Manageable, Clearable {

  // General Rules to follow in this class
  // 1) Values could be ObjectIDs. In shared mode, always do a lookup before returning to outside world.
  // 2) If you do a lookup, try to store back the actual object into the Map if the key is available.
  // 3) Be careful about existing iterators. It shouldn't throw exception because of (2)
  // 4) When you do so, call markAccessed() to make the cache eviction correct
  // 5) Check write access before any shared changed
  // 6) Call logical Invoke before changin internal state so that NonPortableExceptions dont modify state.
  //

  // TODO:: markAccessed()
  private volatile transient TCObject $__tc_MANAGED;

  public HashMapTC() {
    super();
  }

  public HashMapTC(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  public HashMapTC(int initialCapacity) {
    super(initialCapacity);
  }

  public HashMapTC(Map map) {
    super(map);
  }

  public void clear() {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        ManagerUtil.logicalInvoke(this, "clear()V", new Object[0]);
        super.clear();
      }
    } else {
      super.clear();
    }
  }

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
   * This method is overriden in LinkedHashMap. so any change here needs to be probagated to LinkedHashMap too.
   */
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
  public boolean equals(Object o) {
    return super.equals(o);
  }

  /*
   * This method is overriden in LinkedHashMapTC. so any change here needs to be propagated to LinkedHashMapTC too.
   * XXX:: This method uses getEntry instead of a get and put to avoid changing the modCount in shared mode
   */
  public Object get(Object key) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        Map.Entry e = getEntry(key);
        return lookUpAndStoreIfNecessary(e);
      }
    } else {
      return super.get(key);
    }
  }

  private Object lookUpAndStoreIfNecessary(Map.Entry e) {
    if (e == null) return null;
    Object value = e.getValue();
    if (value instanceof ObjectID) {
      Object newVal = ManagerUtil.lookupObject((ObjectID) value);
      e.setValue(newVal);
      return newVal;
    }
    return value;
  }

  private static Object lookUpIfNecessary(Object o) {
    if (o instanceof ObjectID) { return ManagerUtil.lookupObject((ObjectID) o); }
    return o;
  }

  // XXX: This uses entrySet iterator and since we fix that, this should work.
  public int hashCode() {
    return super.hashCode();
  }

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

  public void putAll(Map map) {
    super.putAll(map);
  }

  public Object remove(Object key) {
    if (__tc_isManaged()) {
      // Managed Version
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        int sizeB4 = size();
        Object orgKey;
        Object val;
        if (key == null) {
          val = super.remove(key);
          orgKey = key;
        } else {
          KeyWrapper kw = new KeyWrapper(key);
          val = super.remove(kw);
          orgKey = kw.getOriginalKey();
        }
        if (sizeB4 != size()) {
          ManagerUtil.logicalInvoke(this, "removeEntryForKey(Ljava/lang/Object;)Ljava/util/HashMap$Entry;",
                                    new Object[] { orgKey });
        }
        return lookUpIfNecessary(val);
      }
    } else {
      return super.remove(key);
    }
  }
  
  /**
   * This method is only to be invoked from the applicator thread. This method does not need to check if the
   * map is managed as it will always be managed when called by the applicator thread. In addition, this method
   * does not need to be synchronized under getResolveLock() as the applicator thread is already under the
   * scope of such synchronization.
   */
  public void __tc_applicator_remove(Object key) {
    if (key == null) {
      super.remove(key);
    } else {
      KeyWrapper kw = new KeyWrapper(key);
      super.remove(kw);
    }
  }

  public Object clone() {
    Manageable clone = (Manageable) super.clone();
    return Util.fixTCObjectReferenceOfClonedObject(this, clone);
  }

  /*
   * This method needs to call logicalInvoke before modifying the local state to avoid inconsistency when throwing
   * NonPortableExceptions TODO:: provide special method for the applicator
   */
  public Object put(Object key, Object value) {
    if (__tc_isManaged()) {
      synchronized (__tc_managed().getResolveLock()) {
        ManagerUtil.checkWriteAccess(this);
        // It sucks todo two lookups
        HashMap.Entry e = getEntry(key);
        if (e == null) {
          // New mapping
          ManagerUtil.logicalInvoke(this, "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", new Object[] {
              key, value });
          return lookUpIfNecessary(super.put(key, value));
        } else {
          // without this, LinkedHashMap will not function properly
          e.recordAccess(this);

          // Replacing old mapping
          Object old = lookUpIfNecessary(e.getValue());
          if (value != old) {
            ManagerUtil.logicalInvoke(this, "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                      new Object[] { e.getKey(), value });
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
   * This method is only to be invoked from the applicator thread. This method does not need to check if the
   * map is managed as it will always be managed when called by the applicator thread. In addition, this method
   * does not need to be synchronized under getResolveLock() as the applicator thread is already under the
   * scope of such synchronization.
   */
  public void __tc_applicator_put(Object key, Object value) {
    super.put(key, value);
  }

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

  public String toString() {
    return super.toString();
  }

  public Set keySet() {
    return new KeySetWrapper(super.keySet());
  }

  public Collection values() {
    return new ValuesCollectionWrapper(super.values());
  }

  public Set entrySet() {
    return nonOverridableEntrySet();
  }

  private Set nonOverridableEntrySet() {
    return new EntrySetWrapper(super.entrySet());
  }

  /**
   * Clearable interface - called by CacheManager thru TCObjectLogical
   */
  public int clearReferences(int toClear) {
    if (!__tc_isManaged()) { throw new AssertionError("clearReferences() called on Unmanaged Map"); }
    synchronized (__tc_managed().getResolveLock()) {
      int cleared = 0;
      for (Iterator i = super.entrySet().iterator(); i.hasNext() && toClear > cleared;) {
        Map.Entry e = (Map.Entry) i.next();
        if (e.getValue() instanceof Manageable) {
          Manageable m = (Manageable) e.getValue();
          TCObject tcObject = m.__tc_managed();
          if (tcObject != null && !tcObject.recentlyAccessed()) {
            e.setValue(tcObject.getObjectID());
            cleared++;
          }
        }
      }
      return cleared;
    }
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
   * This wrapper depends on the fact that key.equals() gets called on the wrapper instead of the otherway around
   */
  private static class KeyWrapper {

    private final Object key;
    private Object       orgKeyInstance;

    public KeyWrapper(Object key) {
      this.key = key;
    }

    public int hashCode() {
      return key.hashCode();
    }

    public Object getOriginalKey() {
      return orgKeyInstance;
    }

    // XXX:: Keys cant be ObjectIDs
    public boolean equals(Object o) {
      if (o == key) {
        orgKeyInstance = key;
        return true;
      } else if (key.equals(o)) {
        orgKeyInstance = o;
        return true;
      } else {
        return false;
      }
    }
  }

  /*
   * This wrapper depends on the fact that key.equals() gets called on the wrapper instead of the otherway around
   */
  static class ValueWrapper {

    private final Object value;

    public ValueWrapper(Object value) {
      this.value = value;
    }

    public int hashCode() {
      return value.hashCode();
    }

    public boolean equals(Object o) {
      Object pojo = lookUpIfNecessary(o); // XXX:: This is not stored in the Map since we dont know the key
      return pojo == value || value.equals(pojo);
    }
  }

  private class EntryWrapper implements Map.Entry {

    private final Map.Entry entry;

    public EntryWrapper(Map.Entry entry) {
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
    public Object getValue() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          Object value = lookUpIfNecessary(entry.getValue());
          if (entry.getValue() != value) {
            entry.setValue(value);
          }
          return value;
        }
      } else {
        return entry.getValue();
      }
    }

    /*
     * Even though we do a lookup of oldVal after we change the value in the transaction, DGC will not be able to kick
     * the oldVal out since the transaction is not committed.
     */
    public Object setValue(Object value) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          ManagerUtil.checkWriteAccess(HashMapTC.this);
          ManagerUtil.logicalInvoke(HashMapTC.this, "put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                    new Object[] { entry.getKey(), value });
          Object oldVal = entry.setValue(value);
          return lookUpIfNecessary(oldVal);
        }
      } else {
        return entry.setValue(value);
      }
    }

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

  private class EntrySetWrapper extends AbstractSet {

    private final Set entries;

    public EntrySetWrapper(Set entries) {
      this.entries = entries;
    }

    public void clear() {
      HashMapTC.this.clear();
    }

    // Has to take care of ObjectIDs
    public boolean contains(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          if (!(o instanceof Map.Entry)) return false;
          Map.Entry e = (Map.Entry) o;
          Object key = e.getKey();
          if (!HashMapTC.this.containsKey(key)) { return false; }
          Object value = HashMapTC.this.get(key);
          return value == e.getValue() || (value != null && value.equals(e.getValue()));
        }
      } else {
        return entries.contains(o);
      }
    }

    public Iterator iterator() {
      return new EntriesIterator(entries.iterator());
    }

    public boolean remove(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          if (!(o instanceof Map.Entry)) return false;
          Map.Entry e = (Map.Entry) o;
          Object key = e.getKey();
          int sizeB4 = size();
          HashMapTC.this.remove(key);
          return (sizeB4 != size());
        }
      } else {
        return entries.remove(o);
      }
    }

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

    public void clear() {
      HashMapTC.this.clear();
    }

    public boolean contains(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return _keySet.contains(o);
        }
      } else {
        return _keySet.contains(o);
      }
    }

    public Iterator iterator() {
      return new KeysIterator(HashMapTC.this.nonOverridableEntrySet().iterator());
    }

    public boolean remove(Object o) {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          // Managed version
          int sizeB4 = size();
          HashMapTC.this.remove(o);
          return (size() != sizeB4);
        }
      } else {
        return _keySet.remove(o);
      }
    }

    public int size() {
      return HashMapTC.this.size();
    }

  }

  private class ValuesCollectionWrapper extends AbstractCollection {

    private final Collection _values;

    public ValuesCollectionWrapper(Collection values) {
      this._values = values;
    }

    public void clear() {
      HashMapTC.this.clear();
    }

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

    public Iterator iterator() {
      return new ValuesIterator(HashMapTC.this.nonOverridableEntrySet().iterator());
    }

    public int size() {
      return HashMapTC.this.size();
    }

  }

  private class EntriesIterator implements Iterator {

    private final Iterator iterator;
    private Map.Entry      currentEntry;

    public EntriesIterator(Iterator iterator) {
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
      currentEntry = nextEntry();
      return new EntryWrapper(currentEntry);
    }

    protected Map.Entry nextEntry() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          return (Map.Entry) iterator.next();
        }
      } else {
        return (Map.Entry) iterator.next();
      }
    }

    public void remove() {
      if (__tc_isManaged()) {
        synchronized (__tc_managed().getResolveLock()) {
          ManagerUtil.checkWriteAccess(HashMapTC.this);
          iterator.remove();
          ManagerUtil.logicalInvoke(HashMapTC.this, "removeEntryForKey(Ljava/lang/Object;)Ljava/util/HashMap$Entry;",
                                    new Object[] { currentEntry.getKey() });
        }
      } else {
        iterator.remove();
      }
    }
  }

  private class KeysIterator extends EntriesIterator {

    public KeysIterator(Iterator iterator) {
      super(iterator);
    }

    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      return e.getKey();
    }
  }

  private class ValuesIterator extends EntriesIterator {

    public ValuesIterator(Iterator iterator) {
      super(iterator);
    }

    public Object next() {
      Map.Entry e = (Map.Entry) super.next();
      return e.getValue();
    }

  }
}
