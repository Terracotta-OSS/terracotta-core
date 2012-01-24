/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.change;

import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.change.event.ArrayElementChangeEvent;
import com.tc.object.change.event.LiteralChangeEvent;
import com.tc.object.change.event.LogicalChangeEvent;
import com.tc.object.change.event.PhysicalChangeEvent;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author orion
 */
public class TCChangeBufferImpl implements TCChangeBuffer {
  private final SetOnceFlag                      dnaCreated = new SetOnceFlag();
  private final TCObject                         tcObject;

  private final Map                              physicalEvents;
  private final List                             logicalEvents;
  private final Map                              arrayEvents;
  private final List                             literalValueChangedEvents;
  private final List<MetaDataDescriptorInternal> metaData;

  public TCChangeBufferImpl(TCObject object) {
    this.tcObject = object;

    // This stuff is slightly yucky, but the "null"-ness of these event collections is relevant for determining whether
    // physical updates to logical classes should be ignore or not
    TCClass clazz = tcObject.getTCClass();
    if (clazz.isIndexed()) {
      physicalEvents = null;
      literalValueChangedEvents = null;
      logicalEvents = null;
      arrayEvents = new LinkedHashMap();
    } else if (clazz.isLogical()) {
      physicalEvents = null;
      literalValueChangedEvents = null;
      logicalEvents = new LinkedList();
      arrayEvents = null;
    } else {
      physicalEvents = new HashMap();
      literalValueChangedEvents = new LinkedList();
      logicalEvents = null;
      arrayEvents = null;
    }

    metaData = new ArrayList<MetaDataDescriptorInternal>();
  }

  public boolean isEmpty() {
    if ((physicalEvents != null) && (!physicalEvents.isEmpty())) { return false; }
    if ((literalValueChangedEvents != null) && (!literalValueChangedEvents.isEmpty())) { return false; }
    if ((logicalEvents != null) && (!logicalEvents.isEmpty())) { return false; }
    if ((arrayEvents != null) && (!arrayEvents.isEmpty())) { return false; }
    if (!metaData.isEmpty()) return false;

    return true;
  }

  public void writeTo(DNAWriter writer) {
    if (dnaCreated.attemptSet()) {
      if (arrayEvents != null) {
        writeEventsToDNA(arrayEvents.values(), writer);
      }
      if (physicalEvents != null) {
        writeEventsToDNA(physicalEvents.values(), writer);
      }
      if (logicalEvents != null) {
        writeEventsToDNA(logicalEvents, writer);
      }
      if (literalValueChangedEvents != null) {
        writeEventsToDNA(literalValueChangedEvents, writer);
      }

      for (MetaDataDescriptorInternal md : metaData) {
        ((DNAWriterInternal) writer).addMetaData(md);
      }

      return;
    }

    throw new IllegalStateException("DNA already created");
  }

  private void writeEventsToDNA(Collection events, DNAWriter writer) {
    if (events.size() > 0) {
      for (Iterator iter = events.iterator(); iter.hasNext();) {
        TCChangeBufferEvent event = (TCChangeBufferEvent) iter.next();
        event.write(writer);
      }
    }
  }

  public void literalValueChanged(Object newValue) {
    literalValueChangedEvents.add(new LiteralChangeEvent(newValue));
  }

  public void fieldChanged(String classname, String fieldname, Object newValue, int index) {
    Assert.eval(newValue != null);

    if (index >= 0) {
      // We could add some form of threshold for array change events where instead of encoding the individual updates,
      // we could just send the data for the entire array (like we do when a managed array is brand new)

      // could use a better collection that maintain put order for repeated additions
      Integer key = Integer.valueOf(index);
      arrayEvents.remove(key);
      arrayEvents.put(key, new ArrayElementChangeEvent(index, newValue));
    } else {
      if (logicalEvents != null) {
        // this shouldn't happen
        throw new AssertionError("Physical field change for " + classname + "." + fieldname + " on "
                                 + tcObject.getTCClass().getName() + " which is logically managed");
      }

      // XXX: only fully qualify fieldnames when necessary (ie. when a variable name is shadowed)
      // XXX: When and if this change is made, you also want to look at GenericTCField
      // fieldname = classname + "." + fieldname;

      // Assert.eval(fieldname.indexOf('.') >= 0);
      // this will replace any existing event for this field. Last change made to any field within the same TXN wins
      physicalEvents.put(fieldname, new PhysicalChangeEvent(fieldname, newValue));
    }
  }

  public void arrayChanged(int startPos, Object array, int newLength) {
    // could use a better collection that maintain put order for repeated additions
    Integer key = Integer.valueOf(-startPos); // negative int is used for sub-arrays
    ArrayElementChangeEvent oldEvent = (ArrayElementChangeEvent) arrayEvents.remove(key);
    if (oldEvent != null) {
      Object oldArray = oldEvent.getValue();
      int oldLength = oldEvent.getLength();
      if (oldLength > newLength) {
        System.arraycopy(array, 0, oldArray, 0, newLength);
        array = oldArray;
      }
    }
    arrayEvents.put(key, new ArrayElementChangeEvent(startPos, array, newLength));
  }

  public void logicalInvoke(int method, Object[] parameters) {
    // TODO: It might be useful (if it doesn't take too much CPU) to collapse logical operations. For instance,
    // if a put() is followed by a remove() on the same key we don't need to send anything. Or if multiple put()s are
    // done, only the last one matters

    logicalEvents.add(new LogicalChangeEvent(method, parameters));
  }

  public TCObject getTCObject() {
    return tcObject;
  }

  public void addMetaDataDescriptor(MetaDataDescriptorInternal md) {
    metaData.add(md);
  }

  public boolean hasMetaData() {
    return !metaData.isEmpty();
  }

}
