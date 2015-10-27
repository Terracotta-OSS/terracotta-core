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

import com.tc.net.GroupID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.test.TCTestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ClusterStateTest extends TCTestCase {
  private ClusterStatePersistor clusterStatePersistor;
  private ConnectionIDFactory connectionIDFactory;
  private StripeIDStateManager stripeIdStateManager;
  private ClusterState                        clusterState;

  @Override
  public void setUp() throws Exception {
    clusterStatePersistor = when(mock(ClusterStatePersistor.class).getGroupId()).thenReturn(GroupID.NULL_ID).getMock();
    connectionIDFactory = mock(ConnectionIDFactory.class);
    stripeIdStateManager = mock(StripeIDStateManager.class);
  }

  public void testRejectDifferentGroupID() throws Exception {
    when(clusterStatePersistor.getGroupId()).thenReturn(new GroupID(0));
    try {
      clusterState = new ClusterStateImpl(clusterStatePersistor, connectionIDFactory,
          new GroupID(1), stripeIdStateManager);
      clusterState.toString();
      fail();
    } catch (IllegalStateException exception) {
      // expected
    }
  }

  public void testAcceptSameGroupID() throws Exception {
    when(clusterStatePersistor.getGroupId()).thenReturn(new GroupID(1));
    clusterState = new ClusterStateImpl(clusterStatePersistor, connectionIDFactory,
        new GroupID(1), stripeIdStateManager);
    clusterState.toString();
    verify(clusterStatePersistor, never()).setGroupId(new GroupID(1));
  }

  public void testSaveNewGroupID() throws Exception {
    clusterState = new ClusterStateImpl(clusterStatePersistor, connectionIDFactory,
        new GroupID(1), stripeIdStateManager);
    clusterState.toString();
    verify(clusterStatePersistor).setGroupId(new GroupID(1));
  }
}
