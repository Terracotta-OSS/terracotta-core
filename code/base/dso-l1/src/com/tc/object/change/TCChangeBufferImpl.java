/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.change;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.change.event.ArrayElementChangeEvent;
import com.tc.object.change.event.LiteralChangeEvent;
import com.tc.object.change.event.LogicalChangeEvent;
import com.tc.object.change.event.PhysicalChangeEvent;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.DNAWriterImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.tx.optimistic.OptimisticTransactionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;

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
  private static final TCLogger logger     = TCLogging.getLogger(TCChangeBuffer.class);

  private final SetOnceFlag     dnaCreated = new SetOnceFlag();
  private final TCObject        tcObject;

  private final int             type;
  private final Map             physicalEvents;
  private final List            logicalEvents;
  private final Map             arrayEvents;
  private final List            literalValueChangedEvents;

  public TCChangeBufferImpl(TCObject object) {
    this.tcObject = object;

    // This stuff is slightly yucky, but the "null"-ness of these event collections is relevant for determining whether
    // physical updates to logical classes should be ignore or not
    TCClass clazz = tcObject.getTCClass();
    if (clazz.isIndexed()) {
      type = ARRAY;
      physicalEvents = null;
      literalValueChangedEvents = null;
      logicalEvents = null;
      arrayEvents = new LinkedHashMap();
    } else if (clazz.isLogical()) {
      type = LOGICAL;
      physicalEvents = null;
      literalValueChangedEvents = null;
      logicalEvents = new LinkedList();
      arrayEvents = null;
    } else {
      type = PHYSICAL;
      physicalEvents = new HashMap();
      literalValueChangedEvents = new LinkedList();
      logicalEvents = null;
      arrayEvents = null;
    }
  }

  public void writeTo(TCByteBufferOutputStream output, ObjectStringSerializer serializer, DNAEncoding encoding) {
    // NOTE: This method releases the change events to conserve memory

    if (dnaCreated.attemptSet()) {
      boolean commitNew = tcObject.getAndResetNew();

      TCClass tcClass = tcObject.getTCClass();
      String className = tcClass.getExtendingClassName();
      String loaderDesc = tcClass.getDefiningLoaderDescription();
      DNAWriter writer = new DNAWriterImpl(output, tcObject.getObjectID(), className, serializer, encoding, loaderDesc,
                                           !commitNew);

      if (commitNew) {
        tcObject.dehydrate(writer);
      } else {
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
      }

      writer.finalizeDNA();
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
      Integer key = new Integer(index);
      arrayEvents.remove(key);
      arrayEvents.put(key, new ArrayElementChangeEvent(index, newValue));
    } else {
      if (logicalEvents != null) {
        // ignore physical updates to classes that are logically managed (This happens for things like THash which is a
        // superclass of THashMap)
        if (logger.isDebugEnabled()) {
          logger.debug("Ignoring physical field change for " + classname + "." + fieldname + " since "
                       + tcObject.getTCClass().getName() + " is logically managed");
        }
        return;
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
    Integer key = new Integer(-startPos); // negative int is used for sub-arrays
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

  public int getTotalEventCount() {
    int eventCount = 0;
    if (physicalEvents != null) {
      eventCount += physicalEvents.size();
    }
    if (literalValueChangedEvents != null) {
      eventCount += literalValueChangedEvents.size();
    }
    if (logicalEvents != null) {
      eventCount += logicalEvents.size();
    }
    if (arrayEvents != null) {
      eventCount += arrayEvents.size();
    }
    return eventCount;
  }

  public int getType() {
    return type;
  }

  public void accept(TCChangeBufferEventVisitor visitor) {
    switch (type) {
      case LOGICAL:
        for (Iterator it = logicalEvents.iterator(); it.hasNext();) {
          visitor.visitLogicalEvent((LogicalChangeEvent) it.next());
        }
        break;

      case PHYSICAL:
        if (literalValueChangedEvents != null && literalValueChangedEvents.size() > 0) { throw new AssertionError(
                                                                                                                  "Changes to literal roots are not supported in OptimisticTransaction."); }
        for (Iterator it = physicalEvents.values().iterator(); it.hasNext();) {
          visitor.visitPhysicalChangeEvent((PhysicalChangeEvent) it.next());
        }
        break;

      case ARRAY:
        for (Iterator it = arrayEvents.values().iterator(); it.hasNext();) {
          visitor.visitArrayElementChangeEvent((ArrayElementChangeEvent) it.next());
        }
        break;

      default:
        throw new AssertionError("Unknown event type " + type);
    }
  }

  public DNACursor getDNACursor(OptimisticTransactionManager transactionManager) {
    switch (type) {
      case PHYSICAL:
        if (literalValueChangedEvents != null && literalValueChangedEvents.size() > 0) { throw new AssertionError(
                                                                                                                  "Changes to literal roots are not supported in OptimisticTransaction."); }
        return new AbstractDNACursor(physicalEvents.values(), transactionManager) {
          Object createNextAction(Object object) {
            PhysicalChangeEvent pe = (PhysicalChangeEvent) object;
            return new PhysicalAction(pe.getFieldName(), convertToParameter(pe.getNewValue()), pe.isReference());
          }
        };

      case LOGICAL:
        return new AbstractDNACursor(logicalEvents, transactionManager) {
          Object createNextAction(Object object) {
            LogicalChangeEvent le = (LogicalChangeEvent) object;
            Object[] p = new Object[le.getParameters().length];
            for (int i = 0; i < le.getParameters().length; i++) {
              p[i] = convertToParameter(le.getParameters()[i]);
            }
            return new LogicalAction(le.getMethodID(), p);
          }
        };

      case ARRAY:
        return new AbstractDNACursor(arrayEvents.values(), transactionManager) {
          Object createNextAction(Object object) {
            ArrayElementChangeEvent ae = (ArrayElementChangeEvent) object;
            if (ae.isSubarray()) {
              return new PhysicalAction(ae.getValue(), ae.getIndex());
            } else {
              return new PhysicalAction(ae.getIndex(), convertToParameter(ae.getValue()), ae.isReference());
            }
          }
        };

      default:
        throw new AssertionError("Unknown event type " + type);
    }
  }

  private static abstract class AbstractDNACursor implements DNACursor {
    private final OptimisticTransactionManager transactionManager;
    private final Iterator                     iterator;
    private final int                          size;

    private Object                             currentAction = null;

    public AbstractDNACursor(Collection values, OptimisticTransactionManager transactionManager) {
      this.transactionManager = transactionManager;
      this.iterator = values.iterator();
      this.size = values.size();
    }

    public boolean next() {
      boolean hasNext = iterator.hasNext();
      if (hasNext) {
        this.currentAction = createNextAction(iterator.next());
      }
      return hasNext;
    }

    abstract Object createNextAction(Object object);

    public boolean next(DNAEncoding encoding) {
      return next();
    }

    public int getActionCount() {
      return size;
    }

    public Object getAction() {
      return currentAction;
    }

    public LogicalAction getLogicalAction() {
      return (LogicalAction) currentAction;
    }

    public PhysicalAction getPhysicalAction() {
      return (PhysicalAction) currentAction;
    }

    public void reset() throws UnsupportedOperationException {
      throw new UnsupportedOperationException("This operation is not supported by this class.");
    }

    protected Object convertToParameter(Object object) {
      return transactionManager.convertToParameter(object);
    }
  }

}
