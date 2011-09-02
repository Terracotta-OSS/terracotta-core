/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;

public interface NoReferencesIDStore {

  public static final NoReferencesIDStore NULL_NO_REFERENCES_ID_STORE = new NoReferencesIDStore() {

                                                                        public void addToNoReferences(ManagedObject mo) {
                                                                          // do nothing
                                                                        }

                                                                        public void clearFromNoReferencesStore(ObjectID id) {
                                                                          // do nothing
                                                                        }

                                                                        public boolean hasNoReferences(ObjectID id) {
                                                                          return false;
                                                                        }
                                                                      };

  void addToNoReferences(ManagedObject mo);

  void clearFromNoReferencesStore(ObjectID id);

  boolean hasNoReferences(ObjectID id);

}
