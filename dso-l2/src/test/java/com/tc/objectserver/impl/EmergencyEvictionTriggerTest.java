/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.ObjectManager;

/**
 *
 * @author mscott
 */
public class EmergencyEvictionTriggerTest extends AbstractEvictionTriggerTest {
    
    public EmergencyEvictionTriggerTest() {
    }

    @Override
    public AbstractEvictionTrigger getTrigger() {
        return new EmergencyEvictionTrigger(Mockito.mock(ObjectManager.class), ObjectID.NULL_ID, false);
    }

}
