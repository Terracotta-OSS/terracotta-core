/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Mockito.mock;
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
        return new EmergencyEvictionTrigger(ObjectID.NULL_ID, 0);
    }

}
