/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSetChangedListener;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;

/**
 *
 * @author mscott
 */
public class CapacityEvictionTriggerTest extends AbstractEvictionTriggerTest {
  
  ServerMapEvictionManager mgr = Mockito.mock(ServerMapEvictionManager.class);
    
    public CapacityEvictionTriggerTest() {
    }

    @Override
    public AbstractEvictionTrigger createTrigger() {
        return new CapacityEvictionTrigger(mgr, ObjectID.NULL_ID);
    }
    
    @Test
    public void testCapacityEvictionChaining() throws Exception {
        //  ten million elements in map
        Mockito.when(getEvictableMap().getSize()).thenReturn(10000000);
        //  set max to 250k
        Mockito.when(getEvictableMap().getMaxTotalCount()).thenReturn(250000);
        checkCycle(250000);
        Mockito.verify(this.getClientSet())
          .addReferenceSetChangeListener(Matchers.<ClientObjectReferenceSetChangedListener> any());
    }

        @Test
    public void testCapacityEvictionStacking() throws Exception {
        final EvictableMap map = getEvictableMap();
        final CapacityEvictionTrigger ct = (CapacityEvictionTrigger)getTrigger();
        final ClientObjectReferenceSet cs = getClientSet();
        //  ten million elements in map
        Mockito.when(map.getSize()).thenReturn(10000000);
        //  set max to 250k
        Mockito.when(map.getMaxTotalCount()).thenReturn(250000);
        Mockito.when(map.getRandomSamples(Matchers.anyInt(), Matchers.<ClientObjectReferenceSet>any(), Matchers.<SamplingType>any()))
                .thenReturn(Collections.<Object, EvictableEntry>emptyMap());

        boolean started = ct.startEviction(map);
        Assert.assertTrue(started);
        Mockito.verify(map).startEviction();
        ServerMapEvictionContext found = ct.collectEvictionCandidates(250000, "MOCK", map, cs);
        Assert.assertNull(found);
        Mockito.verify(cs)
          .addReferenceSetChangeListener(Matchers.<ClientObjectReferenceSetChangedListener> any());
//  now pretend that client updated very fast 
        ct.notifyReferenceSetChanged();
//  now pretend that client updated very fast 
        ct.notifyReferenceSetChanged();
//  now pretend that client updated very fast 
        ct.notifyReferenceSetChanged();
//  happens once
        Mockito.verify(cs)
          .removeReferenceSetChangeListener(Matchers.<ClientObjectReferenceSetChangedListener> any());
//  happens once
        Mockito.verify(mgr).doEvictionOn(ct);
// simulate eviction start on new thread
        Thread es = new Thread() {

          @Override
          public void run() {
            ct.startEviction(map);
// confirm repeating the same trigger            
            Assert.assertTrue(ct.isValid());
// happened once before start was allowed to continue            
            Mockito.verify(map,Mockito.times(2)).startEviction();
            
            Mockito.when(map.getRandomSamples(Matchers.anyInt(), Matchers.<ClientObjectReferenceSet>any(), Matchers.<SamplingType>any()))
                .thenReturn(Collections.<Object, EvictableEntry>singletonMap("test",Mockito.mock(EvictableEntry.class)));
            
            ct.collectEvictionCandidates(250000, "MOCK", map, cs);
            ct.completeEviction(map);
            Mockito.verify(map, Mockito.times(2)).getRandomSamples(Matchers.anyInt(), Matchers.eq(cs), Matchers.eq(SamplingType.FOR_EVICTION));
// only once
            Mockito.verify(cs).addReferenceSetChangeListener(Matchers.<ClientObjectReferenceSetChangedListener> any());
          }
          
        };

        es.start();   
        
        TimeUnit.SECONDS.sleep(3);
// make sure getRandomSamples hasn't been called again
        Mockito.verify(map).getRandomSamples(Matchers.anyInt(), Matchers.eq(cs), Matchers.eq(SamplingType.FOR_EVICTION));
        
        ct.completeEviction(map);
        es.join();
    }
        
    @Override @Before
    public void setUp() {
        Mockito.when(getEvictableMap().getSize()).thenReturn(250);
        super.setUp();
    }
    
    
}
