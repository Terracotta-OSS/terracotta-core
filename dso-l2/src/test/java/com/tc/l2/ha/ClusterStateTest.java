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
