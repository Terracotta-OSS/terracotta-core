/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.change;

import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.object.change.event.LogicalChangeEvent;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAWriterInternal;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.util.concurrent.SetOnceFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author orion
 */
public class TCChangeBufferImpl implements TCChangeBuffer {
  private final SetOnceFlag                      dnaCreated = new SetOnceFlag();
  private final TCObject                         tcObject;

  private final List<LogicalChangeEvent>         logicalEvents;
  private final List<MetaDataDescriptorInternal> metaData;

  public TCChangeBufferImpl(TCObject object) {
    this.tcObject = object;
    logicalEvents = new ArrayList<LogicalChangeEvent>();
    metaData = new ArrayList<MetaDataDescriptorInternal>();
  }

  @Override
  public boolean isEmpty() {
    return logicalEvents.isEmpty() && metaData.isEmpty();
  }

  @Override
  public void writeTo(DNAWriter writer) {
    if (dnaCreated.attemptSet()) {
      writeEventsToDNA(logicalEvents, writer);

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

  @Override
  public void literalValueChanged(Object newValue) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void fieldChanged(String classname, String fieldname, Object newValue, int index) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void arrayChanged(int startPos, Object array, int newLength) {
    throw new AssertionError(); // XXX: remove method when possible
  }

  @Override
  public void logicalInvoke(LogicalOperation method, Object[] parameters, LogicalChangeID id) {
    // TODO: It might be useful (if it doesn't take too much CPU) to collapse logical operations. For instance,
    // if a put() is followed by a remove() on the same key we don't need to send anything. Or if multiple put()s are
    // done, only the last one matters
    logicalEvents.add(new LogicalChangeEvent(method, parameters, id));
  }

  @Override
  public TCObject getTCObject() {
    return tcObject;
  }

  @Override
  public void addMetaDataDescriptor(MetaDataDescriptorInternal md) {
    metaData.add(md);
  }

  @Override
  public boolean hasMetaData() {
    return !metaData.isEmpty();
  }

}
