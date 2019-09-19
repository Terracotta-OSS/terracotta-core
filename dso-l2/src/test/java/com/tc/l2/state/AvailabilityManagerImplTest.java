/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.l2.state;

import com.tc.l2.ha.RandomWeightGenerator;
import com.tc.l2.ha.WeightGeneratorFactory;
import com.tc.net.NodeID;
import com.tc.util.Assert;
import java.security.SecureRandom;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import static org.mockito.Mockito.mock;

import org.junit.contrib.java.lang.system.ExpectedSystemExit;

/**
 *
 */
public class AvailabilityManagerImplTest {

  @Rule
  public final ExpectedSystemExit exit = ExpectedSystemExit.none();

  public AvailabilityManagerImplTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   */
  @Test
  public void testCompatiblityMode() throws Exception {
    AvailabilityManagerImpl impl = new AvailabilityManagerImpl(true);
    SecureRandom generator = new SecureRandom();
    WeightGeneratorFactory factory = new WeightGeneratorFactory();
    factory.add(new RandomWeightGenerator(generator, true));
    factory.add(new RandomWeightGenerator(generator, true));
    factory = factory.complete();
    Enrollment e = impl.createVerificationEnrollment(mock(NodeID.class), factory);
    long[] weights = e.getWeights();
    Assert.assertEquals(2, weights.length);
    Assert.assertEquals(Long.MAX_VALUE, weights[0]);
    Assert.assertEquals(0L, weights[1]);
    impl = new AvailabilityManagerImpl(false);
    e = impl.createVerificationEnrollment(mock(NodeID.class), factory);
    weights = e.getWeights();
    Assert.assertEquals(2, weights.length);
    Assert.assertEquals(Long.MAX_VALUE, weights[0]);
    Assert.assertEquals(Long.MAX_VALUE, weights[1]);
  }
}
