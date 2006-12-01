/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.perf.collections;

import com.tctest.perf.collections.ElementType.Factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public interface CollectionType {
  void add(int count, ElementType.Factory typeFactory);

  void remove(int count);

  void sort();

  void iterate();

  Vector getRandom(int count);

  Iterator getValues();

  String describeType();

  int size();

  void clear();

  boolean isSorted();

  void setSorted(boolean yesno);

  abstract public static class CollectionImpl implements CollectionType {

    protected Object collect;
    private boolean  sorted = false;

    public CollectionImpl() {
      // whatever, Eclipse
    }

    public boolean isSorted() {
      return sorted;
    }

    public void setSorted(boolean yesno) {
      sorted = yesno;
    }

    public CollectionImpl(Collection c) {
      collect = c;
    }

    public Collection asCollection() {
      return (Collection) collect;
    }

    public void clear() {
      asCollection().clear();
    }

    public int size() {
      int sz = 0;
      synchronized (collect) {
        sz = asCollection().size();
      }
      return sz;
    }

    public void add(int count, Factory typeFactory) {
      Collection c = asCollection();
      setSorted(false);
      for (int i = 0; i < count; i++)
        c.add(typeFactory.create());
    }

    public void remove(int count) {
      setSorted(false);
      Vector toRemove = getRandom(count);
      asCollection().removeAll(toRemove);
    }

    public void sort() {
      setSorted(true);
      // default implementation does nothing
    }

    public Vector getRandom(int count) {
      Iterator it = getValues();
      Vector ret = new Vector(count);
      for (int i = 0; it.hasNext() && (i < count); i++)
        ret.add(it.next());
      return ret;
    }

    public Iterator getValues() {
      return asCollection().iterator();
    }

    public void iterate() {
      for (Iterator it = getValues(); it.hasNext();) {
        ((ElementType) it.next()).traverse();
      }
    }
  }

  abstract public static class ListCollection extends CollectionImpl {
    public ListCollection() {
      super();
    }

    public void sort() {
      setSorted(true);
      Collections.sort((List) collect);
    }

    public void clear() {
      asList().clear();
    }

    protected List asList() {
      return (List) collect;
    }

    public Vector getRandom(int count) {
      int[] indices = new int[count];
      Random rand = new Random();
      for (int i = 0; i < count; i++)
        indices[i] = rand.nextInt(count);
      Vector ret = new Vector(count);
      List l = asList();
      for (int i = 0; i < count; i++)
        ret.add(l.get(indices[i]));

      return ret;
    }
  }

  public static class VectorCollection extends ListCollection {
    public VectorCollection() {
      super();
      collect = new Vector();
    }

    public String describeType() {
      return "Vector";
    }
  }

  public static class ArrayListCollection extends ListCollection {
    public ArrayListCollection() {
      super();
      collect = new ArrayList();
    }

    public String describeType() {
      return "ArrayList";
    }
  }

  public static class LinkedListCollection extends ListCollection {
    public LinkedListCollection() {
      super();
      collect = new LinkedList();
    }

    public String describeType() {
      return "LinkedList";
    }
  }

  abstract static public class SetCollection extends ListCollection {
    public Vector getRandom(int cnt) {
      Iterator it = getValues();
      Vector res = new Vector(cnt);
      for (int i = 0; (i < cnt) && it.hasNext(); i++)
        res.add(it.next());
      return res;
    }

    public Iterator getValues() {
      return asSet().iterator();
    }

    public void sort() {
      // default implementation does nothing
    }

    Set asSet() {
      return (Set) collect;
    }

    public void clear() {
      asSet().clear();
    }

    public boolean isSorted() {
      return true; // lie to prevent sorting
    }
  }

  public static class HashSetCollection extends SetCollection {
    public HashSetCollection() {
      super();
      collect = new HashSet();
    }

    public String describeType() {
      return "HashSet";
    }
  }

  public static class TreeSetCollection extends SetCollection {
    public TreeSetCollection() {
      super();
      collect = new TreeSet();
    }

    public String describeType() {
      return "TreeSet";
    }
  }

  abstract public static class MapCollection extends CollectionImpl {

    protected ElementType.Factory keyFactory = new ElementType.LongFactory();

    public void add(int count, ElementType.Factory valueFactory) {
      Map me = asMap();
      for (int i = 0; i < count; i++)
        me.put(keyFactory.create(), valueFactory.create());
    }

    public MapCollection() {
      super();
    }

    public boolean isSorted() {
      return true; // lie to prevent sorting
    }

    protected Map asMap() {
      return (Map) collect;
    }

    public void clear() {
      asMap().clear();
    }

    public int size() {

      int sz = 0;
      synchronized (collect) {
        sz = asMap().values().size();
      }
      return sz;
    }

    // we want keys here as the usage is to remove some keyed things
    public Vector getRandom(int count) {
      Vector ret = new Vector(count);
      Iterator keys = asMap().keySet().iterator();
      for (int i = 0; keys.hasNext() && (i < count); i++)
        ret.add(keys.next());
      return ret;
    }

    public void remove(int count) {
      Iterator removeKeys = getRandom(count).iterator();
      while (removeKeys.hasNext())
        asMap().remove(removeKeys.next());
    }

    public Iterator getValues() {
      return asMap().values().iterator();
    }
  }

  public static class HashMapCollection extends MapCollection {
    public HashMapCollection() {
      super();
      collect = new HashMap();
    }

    public String describeType() {
      return "HashMap";
    }
  }

  public static class TreeMapCollection extends MapCollection {
    public TreeMapCollection() {
      super();
      collect = new TreeMap();
    }

    public String describeType() {
      return "TreeMap";
    }
  }

  public static class IdentityHashMapCollection extends MapCollection {
    public IdentityHashMapCollection() {
      super();
      collect = new IdentityHashMap();
    }

    public String describeType() {
      return "IdentityHashMap";
    }
  }

  public static class HashtableCollection extends MapCollection {
    public HashtableCollection() {
      super();
      collect = new Hashtable();
    }

    public String describeType() {
      return "Hashtable";
    }
  }

}
