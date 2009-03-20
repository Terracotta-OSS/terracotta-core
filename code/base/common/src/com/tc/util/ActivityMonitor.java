/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.logging.LogLevelImpl;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class can be used while debugging to record some activity based on a key and later can be used to inpect the
 * activity when something goes wrong. Do note that this class can bloat up the memory and take up a lot of resources.
 * SoftREferences are used to avoid "OutOfMemory" Errors.
 */
public class ActivityMonitor {

  private static final TCLogger logger         = TCLogging.getLogger(ActivityMonitor.class);
  private static Map            activyMonitors = new HashMap();

  private final String          name;
  private final Map             activityMap    = Collections.synchronizedMap(new HashMap());
  private final boolean         softref;

  static {
    // Since this is only used when debugging...
    logger.setLevel(LogLevelImpl.DEBUG);
  }

  public ActivityMonitor() {
    this("Activity Monitor");
  }

  public ActivityMonitor(String name) {
    this(name, true);
  }

  public ActivityMonitor(String name, boolean softref) {
    this.name = name;
    this.softref = softref;
  }

  public void addActivity(Object id, String what) {
    if (softref) {
      addActivitySoftRef(id, what);
    } else {
      addActivityNormal(id, what);
    }
  }

  private void addActivitySoftRef(Object id, String what) {
    synchronized (activityMap) {
      SoftReference ref = (SoftReference) activityMap.get(id);
      if (ref == null) {
        ref = new SoftReference(new LinkedList());
        activityMap.put(id, ref);
      }
      List list = (List) ref.get();
      if (list == null) {
        logger.debug(name + " :: DGC cleared activity for " + id + "!");
        list = new LinkedList();
        list.add("DGC cleared activity ! - (" + new Date() + ")");
        ref = new SoftReference(list);
        activityMap.put(id, ref);
      }
      addActivityList(list, what);
    }
  }

  private void addActivityList(List list, String what) {
    synchronized (list) {
      list.add(what + " - " + Thread.currentThread().getName() + " @ " + new Date());
      try {
        throw new Exception(" happened at -");
      } catch (Exception ex) {
        list.add(ex.getStackTrace());
      }
    }
  }

  private void addActivityNormal(Object id, String what) {
    synchronized (activityMap) {
      List list = (List) activityMap.get(id);
      if (list == null) {
        list = new LinkedList();
        activityMap.put(id, list);
      }
      addActivityList(list, what);
    }
  }

  public void printActivityFor(Object id) {
    if (softref) {
      printActivityForSoftRef(id);
    } else {
      printActivityForNormal(id);
    }
  }

  private void printActivityForSoftRef(Object id) {
    SoftReference ref = (SoftReference) activityMap.get(id);
    if (ref == null) {
      logger.debug(name + " :: No Activity for " + id);
      return;
    }
    List list = (List) ref.get();
    if (list == null) {
      logger.debug(name + " :: DGC cleared Activity for " + id + " !!!");
      return;

    }
    printActivityForList(list, id);
  }

  private void printActivityForNormal(Object id) {
    List list = (List) activityMap.get(id);
    if (list == null) {
      logger.debug(name + " :: No Activity for " + id);
      return;
    }
    printActivityForList(list, id);
  }

  private void printActivityForList(List list, Object id) {
    logger.debug(name + " :: Activity for " + id + " ---- START ----");
    synchronized (list) {
      for (Iterator iter = list.iterator(); iter.hasNext();) {
        Object element = iter.next();
        if (element instanceof StackTraceElement[]) {
          StackTraceElement ste[] = (StackTraceElement[]) element;
          for (int i = 0; i < ste.length; i++) {
            final String steStr = ste[i].toString();
            if (steStr.indexOf(this.getClass().getName()) == -1) {
              logger.debug("\tat " + steStr);
            }
          }
        } else {
          logger.debug(element + " happened at ");
        }
      }
      logger.debug(name + " :: Activity for " + id + " ---- END ----");
    }
  }

  public void clear() {
    activityMap.clear();
  }

  public static ActivityMonitor getActivityMonitor(String name) {
    return getActivityMonitor(name, true);
  }

  public static ActivityMonitor getActivityMonitor(String name, boolean softref) {
    synchronized (activyMonitors) {
      ActivityMonitor monitor = (ActivityMonitor) activyMonitors.get(name);
      if (monitor == null) {
        monitor = new ActivityMonitor(name, softref);
        activyMonitors.put(name, monitor);
      }
      return monitor;
    }
  }
}
