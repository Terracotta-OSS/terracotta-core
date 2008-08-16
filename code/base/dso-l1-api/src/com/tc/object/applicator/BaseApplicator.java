/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.applicator;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.dna.api.DNAEncoding;

/**
 * This class provides facilities for use in implementing applicators.
 */
public abstract class BaseApplicator implements ChangeApplicator {

  private static final TCLogger      logger   = TCLogging.getLogger(BaseApplicator.class);
  private static final LiteralValues literals = createLiteralValuesInstance();

  /**
   * The encoding to use when reading/writing DNA
   */
  protected final DNAEncoding        encoding;

  private static final LiteralValues createLiteralValuesInstance() {
    try {
      Class klazz = Class.forName("com.tc.object.LiteralValues");
      return (LiteralValues) klazz.newInstance();
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    } catch (InstantiationException e) {
      throw new Error(e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }

  /**
   * Construct a BaseApplicator with an encoding to use when reading/writing DNA
   * 
   * @param encoding DNA encoding to use
   */
  protected BaseApplicator(DNAEncoding encoding) {
    this.encoding = encoding;
  }

  /**
   * Get an ObjectID or literal value for the given pojo
   * 
   * @param pojo Object instance
   * @param objectManager Client-side object manager
   * @return ObjectID representing pojo, or the pojo itself if its a literal, or null if it's a non-portable object
   */
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

  /**
   * Determine whether the pojo is a literal instance
   * 
   * @param pojo Object to examine
   * @return True if literal
   */
  protected final boolean isLiteralInstance(Object pojo) {
    return literals.isLiteralInstance(pojo);
  }

  /**
   * Determine whether this class is portable
   * 
   * @param c The class
   * @return True if portable
   */
  protected boolean isPortableReference(Class c) {
    return !literals.isLiteral(c.getName());
  }

}
