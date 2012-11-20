/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.EvictionTrigger;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author mscott
 */
public class AbstractEvictionTriggerTest {
    
    private EvictableMap evm;
    private EvictionTrigger trigger;
    private ClientObjectReferenceSet clientSet;
    
    public EvictableMap getEvictableMap() {
        if ( evm == null ) {
            evm = Mockito.mock(EvictableMap.class);
        }
        return evm;
    }
    
    public EvictionTrigger getTrigger() {
        if ( trigger == null ) {
            trigger = createTrigger();
        }
        return trigger;
    }
    
    public ClientObjectReferenceSet getClientSet() {
        if ( clientSet == null ) {
            clientSet = Mockito.mock(ClientObjectReferenceSet.class);
        }
        return clientSet;
    }
            
    public EvictionTrigger createTrigger() {
        return new AbstractEvictionTrigger(ObjectID.NULL_ID) {

            @Override
            public Map<Object, ObjectID> collectEvictonCandidates(int targetMax, EvictableMap map, ClientObjectReferenceSet clients) {
                return processSample(map.getRandomSamples(targetMax, clientSet));
            }
        };
    }
    
    public AbstractEvictionTriggerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    //
    }
    
    @AfterClass
    public static void tearDownClass() {
    //
    }
    
    @Test
    public void testTriggerLifecycle() {
        int size = 100;
        EvictableMap map = getEvictableMap();
        EvictionTrigger et = getTrigger();
        ClientObjectReferenceSet cs = getClientSet();
        
        if ( et.startEviction(map) ) {
            Map<Object, ObjectID> found = et.collectEvictonCandidates(size, map, cs);
            et.completeEviction(map);
        }
        Mockito.verify(map).startEviction();
    Mockito.verify(map).getRandomSamples(Matchers.anyInt(), Matchers.eq(cs));
        Mockito.verify(map).evictionCompleted();
    }
    
    @Before
    public void setUp() {
        evm = getEvictableMap();
        trigger = getTrigger();
        Mockito.when(evm.startEviction()).thenReturn(Boolean.TRUE);
    Mockito.when(evm.getRandomSamples(Matchers.anyInt(), Matchers.eq(clientSet)))
        .thenReturn(Collections.<Object, ObjectID> emptyMap());
    }
    
    @After
    public void tearDown() {
    //
    }
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}
}
