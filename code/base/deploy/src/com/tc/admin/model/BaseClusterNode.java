/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.swing.event.EventListenerList;

public abstract class BaseClusterNode implements IClusterNode {
  protected PropertyChangeSupport                   propertyChangeSupport;
  protected Map<PolledAttribute, EventListenerList> polledAttributeListenerMap;
  protected Map<ObjectName, Set<String>>            polledAttributeSourceMap;
  protected Map<String, PolledAttribute>            polledAttributeMap;

  protected BaseClusterNode() {
    propertyChangeSupport = new PropertyChangeSupport(this);
    polledAttributeListenerMap = new HashMap<PolledAttribute, EventListenerList>();
    polledAttributeSourceMap = new HashMap<ObjectName, Set<String>>();
    polledAttributeMap = new HashMap<String, PolledAttribute>();
  }

  public synchronized void addPolledAttributeListener(String name, PolledAttributeListener listener) {
    PolledAttribute polledAttribute = getPolledAttribute(name);
    if (polledAttribute != null) {
      addPolledAttributeListener(polledAttribute, listener);
    } else {
      System.err.println("PolledAttribute named '" + name + "' not found in node '" + this + "'");
      Thread.dumpStack();
    }
  }

  public synchronized void addPolledAttributeListener(Set<String> names, PolledAttributeListener listener) {
    Iterator<String> iter = names.iterator();
    while (iter.hasNext()) {
      addPolledAttributeListener(iter.next(), listener);
    }
  }

  public synchronized void addPolledAttributeListener(PolledAttribute polledAttribute, PolledAttributeListener listener) {
    addPolledAttributeListener(polledAttribute.getObjectName(), Collections.singleton(polledAttribute.getAttribute()),
                               listener);
  }

  public synchronized void addPolledAttributeListener(ObjectName objectName, Set<String> attributeSet,
                                         PolledAttributeListener listener) {
    assert attributeSet != null;

    Iterator<String> attributeIter = attributeSet.iterator();
    while (attributeIter.hasNext()) {
      String attribute = attributeIter.next();
      PolledAttribute polledAttribute = new PolledAttribute(objectName, attribute);
      EventListenerList attributeListenerList = polledAttributeListenerMap.get(polledAttribute);
      if (attributeListenerList == null) {
        polledAttributeListenerMap.put(polledAttribute, attributeListenerList = new EventListenerList());
      }
      attributeListenerList.remove(PolledAttributeListener.class, listener);
      attributeListenerList.add(PolledAttributeListener.class, listener);
    }
    Set<String> sourceAttrSet = polledAttributeSourceMap.get(objectName);
    if (sourceAttrSet == null) {
      polledAttributeSourceMap.put(objectName, sourceAttrSet = new HashSet<String>());
    }
    sourceAttrSet.addAll(attributeSet);
  }

  public synchronized void removePolledAttributeListener(String name, PolledAttributeListener listener) {
    PolledAttribute polledAttribute = getPolledAttribute(name);
    if (polledAttribute != null) {
      removePolledAttributeListener(polledAttribute, listener);
    } else {
      System.err.println("PolledAttribute named '" + name + "' not found in node '" + this + "'");
      Thread.dumpStack();
    }
  }

  public synchronized void removePolledAttributeListener(Set<String> names, PolledAttributeListener listener) {
    Iterator<String> iter = names.iterator();
    while (iter.hasNext()) {
      removePolledAttributeListener(iter.next(), listener);
    }
  }

  public synchronized void removePolledAttributeListener(PolledAttribute polledAttribute, PolledAttributeListener listener) {
    removePolledAttributeListener(polledAttribute.getObjectName(), Collections
        .singleton(polledAttribute.getAttribute()), listener);
  }

  public synchronized void removePolledAttributeListener(ObjectName objectName, Set<String> attributeSet,
                                            PolledAttributeListener listener) {
    if (attributeSet != null && listener != null) {
      Iterator<String> attributeIter = attributeSet.iterator();
      while (attributeIter.hasNext()) {
        String attribute = attributeIter.next();
        PolledAttribute polledAttribute = new PolledAttribute(objectName, attribute);
        EventListenerList attributeListenerList = polledAttributeListenerMap.get(polledAttribute);
        if (attributeListenerList != null) {
          attributeListenerList.remove(PolledAttributeListener.class, listener);
        }
        if (attributeListenerList.getListenerCount() == 0) {
          polledAttributeListenerMap.remove(polledAttribute);
          Set<String> sourceAttrSet = polledAttributeSourceMap.get(objectName);
          sourceAttrSet.remove(attribute);
        }
      }
    }
  }

  public synchronized Map<ObjectName, Set<String>> getPolledAttributes() {
    return new HashMap<ObjectName, Set<String>>(polledAttributeSourceMap);
  }

  public synchronized Set<PolledAttributeListener> getPolledAttributeListeners() {
    Iterator<EventListenerList> iter = polledAttributeListenerMap.values().iterator();
    Set<PolledAttributeListener> result = new HashSet<PolledAttributeListener>();
    while (iter.hasNext()) {
      Collections.addAll(result, iter.next().getListeners(PolledAttributeListener.class));
    }
    return result;
  }

  protected void registerPolledAttribute(PolledAttribute polledAttribute) {
    registerPolledAttribute(polledAttribute.getAttribute(), polledAttribute);
  }

  protected void registerPolledAttribute(String name, PolledAttribute polledAttribute) {
    polledAttributeMap.put(name, polledAttribute);
  }

  public PolledAttribute getPolledAttribute(String name) {
    return polledAttributeMap.get(name);
  }

  public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
      propertyChangeSupport.addPropertyChangeListener(listener);
    }
  }

  public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    if (listener != null && propertyChangeSupport != null) {
      propertyChangeSupport.removePropertyChangeListener(listener);
    }
  }

  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    PropertyChangeSupport pcs;
    synchronized (this) {
      pcs = propertyChangeSupport;
    }
    if (pcs != null && (oldValue != null || newValue != null)) {
      pcs.firePropertyChange(propertyName, oldValue, newValue);
    }
  }

  public synchronized void tearDown() {
    polledAttributeListenerMap.clear();
    polledAttributeListenerMap = null;

    polledAttributeSourceMap.clear();
    polledAttributeSourceMap = null;

    polledAttributeMap.clear();
    polledAttributeMap = null;

    if (propertyChangeSupport != null) {
      PropertyChangeListener[] listeners = propertyChangeSupport.getPropertyChangeListeners();
      if (listeners != null) {
        for (PropertyChangeListener listener : listeners) {
          propertyChangeSupport.removePropertyChangeListener(listener);
        }
      }
      propertyChangeSupport = null;
    }
  }
}
