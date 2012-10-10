/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.gb;

import com.tc.util.sequence.ObjectIDSequence;

/**
 *
 * @author mscott
 */
public class GBObjectIDSequence implements ObjectIDSequence {
    
    final GBManagedObjectPersistor persistor;

    public GBObjectIDSequence(GBManagedObjectPersistor persistor) {
        this.persistor = persistor;
    }
    
    

    @Override
    public long nextObjectIDBatch(int batchSize) {
        return persistor.nextObjectIDBatch(batchSize);
    }

    @Override
    public void setNextAvailableObjectID(long startID) {
        persistor.setNextAvailableObjectID(startID);
    }

    @Override
    public long currentObjectIDValue() {
        return persistor.currentObjectIDValue();
    }
    
}
