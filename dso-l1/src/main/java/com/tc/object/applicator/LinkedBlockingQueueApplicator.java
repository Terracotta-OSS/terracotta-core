/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.SerializationUtil;
import com.tc.object.TCObjectExternal;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.util.Assert;
import com.tc.util.FieldUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueApplicator extends BaseApplicator {
  private static final TCLogger logger                                  = TCLogging
                                                                            .getLogger(LinkedBlockingQueueApplicator.class);
  private static final String   TAKE_LOCK_FIELD_NAME                    = "takeLock";
  private static final String   PUT_LOCK_FIELD_NAME                     = "putLock";
  private static final String   CAPACITY_FIELD_NAME                     = "capacity";
  private static final String   INIT_METHOD_NAME                        = "init";
  private static final String   TC_TAKE_METHOD_NAME                     = ByteCodeUtil.TC_METHOD_PREFIX + "take";
  private static final String   TC_PUT_METHOD_NAME                      = ByteCodeUtil.TC_METHOD_PREFIX + "put";

  private static final Field    TAKE_LOCK_FIELD;
  private static final Field    PUT_LOCK_FIELD;
  private static final Field    CAPACITY_FIELD;
  private static final Method   INIT_METHOD;
  private static final Method   TC_TAKE_METHOD;
  private static final Method   TC_PUT_METHOD;

  static {
    try {
      TAKE_LOCK_FIELD = LinkedBlockingQueue.class.getDeclaredField(TAKE_LOCK_FIELD_NAME);
      TAKE_LOCK_FIELD.setAccessible(true);

      PUT_LOCK_FIELD = LinkedBlockingQueue.class.getDeclaredField(PUT_LOCK_FIELD_NAME);
      PUT_LOCK_FIELD.setAccessible(true);

      CAPACITY_FIELD = LinkedBlockingQueue.class.getDeclaredField(CAPACITY_FIELD_NAME);
      CAPACITY_FIELD.setAccessible(true);

      INIT_METHOD = LinkedBlockingQueue.class.getDeclaredMethod(INIT_METHOD_NAME, new Class[0]);
      INIT_METHOD.setAccessible(true);

      TC_TAKE_METHOD = LinkedBlockingQueue.class.getDeclaredMethod(TC_TAKE_METHOD_NAME, new Class[0]);
      TC_TAKE_METHOD.setAccessible(true);

      TC_PUT_METHOD = LinkedBlockingQueue.class.getDeclaredMethod(TC_PUT_METHOD_NAME, new Class[] { Object.class });
      TC_PUT_METHOD.setAccessible(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public LinkedBlockingQueueApplicator(DNAEncoding encoding, TCLogger logger) {
    super(encoding, logger);
  }

  public TraversedReferences getPortableObjects(Object pojo, TraversedReferences addTo) {
    getPhysicalPortableObjects(pojo, addTo);
    getLogicalPortableObjects(pojo, addTo);
    return addTo;
  }

  private void getLogicalPortableObjects(Object pojo, TraversedReferences addTo) {
    for (Iterator i = ((Queue) pojo).iterator(); i.hasNext();) {
      Object o = i.next();
      filterPortableObject(o, addTo);
    }
  }

  private void getPhysicalPortableObjects(Object pojo, TraversedReferences addTo) {
    try {
      filterPortableObject(TAKE_LOCK_FIELD.get(pojo), addTo);
      filterPortableObject(PUT_LOCK_FIELD.get(pojo), addTo);
      filterPortableObject(CAPACITY_FIELD.get(pojo), addTo);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void filterPortableObject(Object value, TraversedReferences addTo) {
    if (value != null && isPortableReference(value.getClass())) {
      addTo.addAnonymousReference(value);
    }
  }

  public void hydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNA dna, Object po)
      throws IOException, ClassNotFoundException {
    LinkedBlockingQueue queue = (LinkedBlockingQueue) po;
    DNACursor cursor = dna.getCursor();
    boolean hasPhysicalAction = false;

    Object takeLock = null;
    Object putLock = null;
    Object capacity = null;
    while (cursor.next(encoding)) {
      Object action = cursor.getAction();
      if (action instanceof LogicalAction) {

        LogicalAction logicalAction = (LogicalAction) action;
        int method = logicalAction.getMethod();
        Object[] params = logicalAction.getParameters();

        // Since LinkedBlockingQueue supports partial collection, params is not inspected for containing object ids

        try {
          apply(queue, method, params);
        } catch (IndexOutOfBoundsException ioobe) {
          logger.error("Error applying update to " + po, ioobe);
        }
      } else if (action instanceof PhysicalAction) {
        if (!hasPhysicalAction) {
          hasPhysicalAction = true;
        }
        PhysicalAction physicalAction = (PhysicalAction) action;
        Assert.eval(physicalAction.isTruePhysical());
        String fieldName = physicalAction.getFieldName();
        Object value = physicalAction.getObject();

        if (fieldName.equals(TAKE_LOCK_FIELD_NAME)) {
          takeLock = objectManager.lookupObject((ObjectID) value);
        } else if (fieldName.equals(PUT_LOCK_FIELD_NAME)) {
          putLock = objectManager.lookupObject((ObjectID) value);
        } else if (fieldName.equals(CAPACITY_FIELD_NAME)) {
          capacity = value;
        }
      }
    }

    // The setting of these physical field can only happen after the logical actions are
    // applied.
    if (!dna.isDelta()) {
      Assert.assertTrue(hasPhysicalAction);
      try {
        FieldUtils.tcSet(po, takeLock, TAKE_LOCK_FIELD);
        FieldUtils.tcSet(po, putLock, PUT_LOCK_FIELD);
        FieldUtils.tcSet(po, capacity, CAPACITY_FIELD);
      } catch (IllegalAccessException e) {
        throw new TCRuntimeException(e);
      }
      invokeInitMethod(po);
    } else {
      Assert.assertFalse(hasPhysicalAction);
    }

  }

  private void invokeInitMethod(Object po) {
    try {
      INIT_METHOD.invoke(po, new Object[0]);
    } catch (InvocationTargetException e) {
      throw new TCRuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void apply(LinkedBlockingQueue queue, int method, Object[] params) {
    switch (method) {
      case SerializationUtil.PUT:
        try {
          TC_PUT_METHOD.invoke(queue, new Object[] { params[0] });
        } catch (InvocationTargetException e) {
          throw new TCRuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new TCRuntimeException(e);
        }
        break;
      case SerializationUtil.TAKE:
        try {
          TC_TAKE_METHOD.invoke(queue, new Object[0]);
        } catch (InvocationTargetException e) {
          throw new TCRuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new TCRuntimeException(e);
        }
        break;
      case SerializationUtil.REMOVE_FIRST_N:
        // This is caused by drainTo(), which requires a full lock.
        int count = ((Integer) params[0]).intValue();
        for (int i = 0; i < count; i++) {
          try {
            TC_TAKE_METHOD.invoke(queue, new Object[0]);
          } catch (InvocationTargetException e) {
            throw new TCRuntimeException(e);
          } catch (IllegalAccessException e) {
            throw new TCRuntimeException(e);
          }
        }
        break;
      case SerializationUtil.REMOVE_AT:
        int index = ((Integer) params[0]).intValue();
        Assert.assertTrue(queue.size() > index);
        int j = 0;
        for (Iterator i = queue.iterator(); i.hasNext();) {
          i.next();
          if (j == index) {
            i.remove();
            break;
          }
          j++;
        }
        break;
      case SerializationUtil.CLEAR:
        queue.clear();
        break;
      default:
        throw new AssertionError("Invalid method:" + method + " state:" + this);
    }
  }

  public void dehydrate(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer, Object pojo) {
    dehydrateFields(objectManager, tcObject, writer, pojo);
    dehydrateMembers(objectManager, tcObject, writer, pojo);
  }

  private void dehydrateFields(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer,
                               Object pojo) {
    try {
      Object takeLock = TAKE_LOCK_FIELD.get(pojo);
      takeLock = getDehydratableObject(takeLock, objectManager);
      writer.addPhysicalAction(TAKE_LOCK_FIELD_NAME, takeLock);

      Object putLock = PUT_LOCK_FIELD.get(pojo);
      putLock = getDehydratableObject(putLock, objectManager);
      writer.addPhysicalAction(PUT_LOCK_FIELD_NAME, putLock);

      Object capacity = CAPACITY_FIELD.get(pojo);
      capacity = getDehydratableObject(capacity, objectManager);
      writer.addPhysicalAction(CAPACITY_FIELD_NAME, capacity);
    } catch (IllegalAccessException e) {
      throw new TCRuntimeException(e);
    }
  }

  private void dehydrateMembers(ApplicatorObjectManager objectManager, TCObjectExternal tcObject, DNAWriter writer,
                                Object pojo) {
    Queue queue = (Queue) pojo;

    for (Iterator i = queue.iterator(); i.hasNext();) {
      Object value = i.next();
      if (!(value instanceof ObjectID)) {
        if (!objectManager.isPortableInstance(value)) {
          continue;
        }
        value = getDehydratableObject(value, objectManager);
      }
      if (value == null) {
        continue;
      }
      writer.addLogicalAction(SerializationUtil.PUT, new Object[] { value });
    }
  }

  public Object getNewInstance(ApplicatorObjectManager objectManager, DNA dna) {
    throw new UnsupportedOperationException();
  }

}
