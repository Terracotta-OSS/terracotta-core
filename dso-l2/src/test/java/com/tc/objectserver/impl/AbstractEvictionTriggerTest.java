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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.tc.object.ObjectID;
import com.tc.objectserver.api.EvictableEntry;
import com.tc.objectserver.api.EvictableMap;
import com.tc.objectserver.context.ServerMapEvictionContext;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;

import java.util.Collections;

/**
 *
 * @author mscott
 */
public class AbstractEvictionTriggerTest {

  private EvictableMap             evm;
  private AbstractEvictionTrigger  trigger;
  private ClientObjectReferenceSet clientSet;

  public EvictableMap getEvictableMap() {
    if (evm == null) {
      evm = Mockito.mock(EvictableMap.class);
      Mockito.when(evm.getMaxTotalCount()).thenReturn(-1);
      Mockito.when(evm.getSize()).thenReturn(100);
    }
    return evm;
  }

  public AbstractEvictionTrigger getTrigger() {
    if (trigger == null) {
      trigger = createTrigger();
    }
    return trigger;
  }

  public ClientObjectReferenceSet getClientSet() {
    if (clientSet == null) {
      clientSet = Mockito.mock(ClientObjectReferenceSet.class);
    }
    return clientSet;
  }

  public AbstractEvictionTrigger createTrigger() {
    return new AbstractEvictionTrigger(ObjectID.NULL_ID) {

      @Override
      public ServerMapEvictionContext collectEvictionCandidates(int targetMax, String className, EvictableMap map,
                                                            ClientObjectReferenceSet clients) {
        return createEvictionContext(className, map.getRandomSamples(boundsCheckSampleSize(targetMax), clientSet,
            SamplingType.FOR_EVICTION));
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

  public void checkSizeCycle(int size) {
    EvictableMap map = getEvictableMap();
    Mockito.when(map.getSize()).thenReturn(size);
    checkCycle(Integer.MAX_VALUE);
  }

  public void checkMaxCycle(int max) {
    EvictableMap map = getEvictableMap();
    Mockito.when(map.getMaxTotalCount()).thenReturn(max);
    checkCycle(max);
  }

  public void checkCycle(int max) {
    EvictableMap map = getEvictableMap();
    final AbstractEvictionTrigger et = getTrigger();
    ClientObjectReferenceSet cs = getClientSet();
    ServerMapEvictionContext found = null;

    boolean startEviction = et.startEviction(map);
    if (startEviction) {
      Mockito.verify(map).startEviction();
      found = et.collectEvictionCandidates(max, "MOCK", map, cs);
      et.completeEviction(map);
    }
    
    if (found != null) {
      Mockito.verify(map).getRandomSamples(Matchers.intThat(new BaseMatcher<Integer>() {
        int maxLocal = et.boundsCheckSampleSize(Integer.MAX_VALUE);

        @Override
        public boolean matches(Object item) {
          if (item instanceof Integer && ((Integer) item) <= maxLocal && ((Integer) item) >= 0) { return true; }
          return false;
        }

        @Override
        public void describeTo(Description description) {
          description.appendText("< " + maxLocal);
        }

      }), Matchers.eq(cs), SamplingType.FOR_EVICTION);
      Mockito.verify(map).evictionCompleted();
    }
  }

  @Test
  public void testNormalMax() {
    checkMaxCycle(200);
  }

  @Test
  public void testPinnedMax() {
    checkMaxCycle(0);
  }

  @Test
  public void testLowerMax() {
    checkMaxCycle(Integer.MIN_VALUE);
  }

  @Test
  public void testUpperMax() {
    checkMaxCycle(Integer.MAX_VALUE);
  }

  @Test
  public void testNormalSize() {
    checkSizeCycle(200);
  }

  @Test
  public void testEmptySize() {
    checkSizeCycle(0);
  }

  @Test
  public void testUpperSize() {
    checkSizeCycle(Integer.MAX_VALUE);
  }

  @Before
  public void setUp() {
    evm = getEvictableMap();
    trigger = getTrigger();
    Mockito.when(evm.startEviction()).thenReturn(Boolean.TRUE);
    Mockito.when(evm.getRandomSamples(Matchers.anyInt(), Matchers.eq(clientSet), Matchers.eq(SamplingType.FOR_EVICTION)))
        .thenReturn(Collections.<Object, EvictableEntry> emptyMap());
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
