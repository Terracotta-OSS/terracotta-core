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
package com.tc.l2.ha;

import org.junit.Assert;

import com.tc.objectserver.persistence.TransactionOrderPersistor;
import com.tc.test.TCTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TransactionCountWeightGeneratorTest extends TCTestCase {
  public void testSimpleProgress() throws Exception {
    TransactionOrderPersistor mockPersistor = mock(TransactionOrderPersistor.class);
    TransactionCountWeightGenerator generator = new TransactionCountWeightGenerator(mockPersistor);
    
    when(mockPersistor.getReceivedTransactionCount()).thenReturn(0L);
    Assert.assertTrue(0L == generator.getWeight());
    when(mockPersistor.getReceivedTransactionCount()).thenReturn(1L);
    Assert.assertTrue(1L == generator.getWeight());
    when(mockPersistor.getReceivedTransactionCount()).thenReturn(4L);
    Assert.assertTrue(4L == generator.getWeight());
    when(mockPersistor.getReceivedTransactionCount()).thenReturn(10L);
    Assert.assertTrue(10L == generator.getWeight());
  }
}
