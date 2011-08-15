/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.api.PersistentCollectionFactory;
import com.tc.objectserver.storage.api.TCMapsDatabase.BackingMapFactory;

import java.util.Map;
import java.util.Set;

public class PersistableCollectionFactory implements PersistentCollectionFactory {

  private final BackingMapFactory factory;
  private final boolean           paranoid;

  public PersistableCollectionFactory(final BackingMapFactory factory, final boolean paranoid) {
    this.factory = factory;
    this.paranoid = paranoid;
  }

  public Map createPersistentMap(final ObjectID id) {
    if(this.paranoid) {
      return new TCPersistableMap(id, this.factory.createBackingMapFor(id));
    } else {
      return new TCPersistableMap(id, this.factory.createBackingMapFor(id), this.factory.createBackingMapFor(id));
    }
  }

  public Set createPersistentSet(final ObjectID id) {
    if(this.paranoid) {
      return new TCPersistableSet(id, this.factory.createBackingMapFor(id));
    } else {
      return new TCPersistableSet(id, this.factory.createBackingMapFor(id), this.factory.createBackingMapFor(id));
    }
  }

}
