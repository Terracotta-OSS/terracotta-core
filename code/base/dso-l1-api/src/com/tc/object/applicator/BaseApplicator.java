/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.util.Assert;

import java.util.Map;

public abstract class BaseApplicator implements ChangeApplicator {

  private static final TCLogger       logger   = TCLogging.getLogger(BaseApplicator.class);
  private static final LiteralValues literals = createLiteralValuesInstance();

  protected final DNAEncoding        encoding;

  private static final LiteralValues createLiteralValuesInstance() {
    try {
      Class klazz = Class.forName("com.tc.object.LiteralValues");
      return (LiteralValues)klazz.newInstance();
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    } catch (InstantiationException e) {
      throw new Error(e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }

  protected BaseApplicator(DNAEncoding encoding) {
    this.encoding = encoding;
  }

  protected final Object getDehydratableObject(Object pojo, ClientObjectManager objectManager) {
    if (pojo == null) {
      return ObjectID.NULL_ID;
    } else if (literals.isLiteralInstance(pojo)) {
      return pojo;
    } else {
      TCObject tcObject = objectManager.lookupExistingOrNull(pojo);
      if (tcObject == null) {
        // When we dehydrate complex objects, traverser bails out on the first non portable
        // object. We dont want to dehydrate things that are not added in the ClientObjectManager.
        logger
            .warn("Not dehydrating object of type " + pojo.getClass().getName() + "@" + System.identityHashCode(pojo));
        return null;
      }
      return tcObject.getObjectID();
    }
  }

  protected final boolean isLiteralInstance(Object pojo) {
    return literals.isLiteralInstance(pojo);
  }

  protected boolean isPortableReference(Class c) {
    return !literals.isLiteral(c.getName());
  }

  protected Object createParentIfNecessary(Map visited, ClientObjectManager objectManager, Map cloned, Object v) {
    return objectManager.createParentCopyInstanceIfNecessary(visited, cloned, v);
  }

  protected Object createCopyIfNecessary(ClientObjectManager objectManager, Map visited, Map cloned, Object originalValue) {
    Object copyKey;
    if (originalValue == null || isLiteralInstance(originalValue)) {
      copyKey = originalValue;
    } else if (visited.containsKey(originalValue)) {
      Assert.eval(visited.get(originalValue) != null);
      copyKey = visited.get(originalValue);
    } else {
      Assert.eval(!isLiteralInstance(originalValue));
      copyKey = objectManager.createNewCopyInstance(originalValue, createParentIfNecessary(visited, objectManager, cloned, originalValue));

      visited.put(originalValue, copyKey);
      cloned.put(originalValue, copyKey);
    }
    return copyKey;
  }

}
