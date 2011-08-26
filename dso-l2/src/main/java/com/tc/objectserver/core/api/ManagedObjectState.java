/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.objectserver.managedobject.ManagedObjectTraverser;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * The internal state of a managed object.
 */
public interface ManagedObjectState {

  /*
   * If you are adding new State objects, you need to add Serialization support in ManagedObjectStateFactory The unit
   * test ManagedObjectStateSerializationTest will also need to have a test case for each of these types, which is
   * defined to be public static final. This will ensure that Serialization support is added to
   * ManagedObjectStateFactory.
   */
  public static final byte PHYSICAL_TYPE                          = 0x01;
  public static final byte DATE_TYPE                              = 0x02;
  public static final byte MAP_TYPE                               = 0x03;
  public static final byte LINKED_HASHMAP_TYPE                    = 0x04;
  public static final byte ARRAY_TYPE                             = 0x05;
  public static final byte LITERAL_TYPE                           = 0x06;
  public static final byte LIST_TYPE                              = 0x07;
  public static final byte SET_TYPE                               = 0x08;
  public static final byte TREE_SET_TYPE                          = 0x09;
  public static final byte TREE_MAP_TYPE                          = 0x0a;
  public static final byte QUEUE_TYPE                             = 0x0b;
  public static final byte CONCURRENT_HASHMAP_TYPE                = 0x0c;
  public static final byte PARTIAL_MAP_TYPE                       = 0x0d;
  public static final byte URL_TYPE                               = 0x0e;
  public static final byte LINKED_HASHSET_TYPE                    = 0x0f;
  public static final byte LINKED_LIST_TYPE                       = 0x10;
  // XXX: hack to get support various tims.
  public static final byte CONCURRENT_DISTRIBUTED_MAP_TYPE        = 0x11;
  public static final byte TDC_SERIALIZED_ENTRY                   = 0x12;
  public static final byte TDC_CUSTOM_LIFESPAN_SERIALIZED_ENTRY   = 0x13;
  public static final byte CONCURRENT_DISTRIBUTED_SERVER_MAP_TYPE = 0x14;

  public void apply(ObjectID objectID, DNACursor cursor, ApplyTransactionInfo applyInfo) throws IOException;

  public Set getObjectReferences();

  public void addObjectReferencesTo(ManagedObjectTraverser traverser);

  public void dehydrate(ObjectID objectID, DNAWriter writer, DNAType type);

  public ManagedObjectFacade createFacade(ObjectID objectID, String className, int limit);

  public byte getType();

  public String getClassName();

  public String getLoaderDescription();

  /**
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  public boolean hasNoReferences();

  public void writeTo(ObjectOutput o) throws IOException;

  // The readFrom() method is currently a static implementation in each state object till I figure out
  // a cleaner way to create physical managed object as each one uses a different class object.
  // public void readFrom(ObjectInput i) throws IOException;
}
