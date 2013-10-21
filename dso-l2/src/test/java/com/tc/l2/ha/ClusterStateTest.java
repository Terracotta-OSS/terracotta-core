package com.tc.l2.ha;

import com.tc.net.GroupID;
import com.tc.net.groups.StripeIDStateManager;
import com.tc.net.protocol.transport.ConnectionIDFactory;
import com.tc.objectserver.gtx.GlobalTransactionIDSequenceProvider;
import com.tc.objectserver.persistence.ClusterStatePersistor;
import com.tc.test.TCTestCase;
import com.tc.util.sequence.DGCSequenceProvider;
import com.tc.util.sequence.ObjectIDSequence;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
public class ClusterStateTest extends TCTestCase {
  private ClusterStatePersistor clusterStatePersistor;
  private ObjectIDSequence oidSequence;
  private ConnectionIDFactory connectionIDFactory;
  private GlobalTransactionIDSequenceProvider gidSequenceProvider;
  private StripeIDStateManager stripeIdStateManager;
  private DGCSequenceProvider dgcSequenceProvider;

  @Override
  public void setUp() throws Exception {
    clusterStatePersistor = when(mock(ClusterStatePersistor.class).getGroupId()).thenReturn(GroupID.NULL_ID).getMock();
    oidSequence = mock(ObjectIDSequence.class);
    connectionIDFactory = mock(ConnectionIDFactory.class);
    gidSequenceProvider = mock(GlobalTransactionIDSequenceProvider.class);
    stripeIdStateManager = mock(StripeIDStateManager.class);
    dgcSequenceProvider = mock(DGCSequenceProvider.class);
  }

  public void testRejectDifferentGroupID() throws Exception {
    when(clusterStatePersistor.getGroupId()).thenReturn(new GroupID(0));
    try {
      new ClusterState(clusterStatePersistor, oidSequence, connectionIDFactory, gidSequenceProvider, new GroupID(1),
          stripeIdStateManager, dgcSequenceProvider);
      fail();
    } catch (AssertionError assertionError) {
      // expected
    }
  }

  public void testAcceptSameGroupID() throws Exception {
    when(clusterStatePersistor.getGroupId()).thenReturn(new GroupID(1));
    new ClusterState(clusterStatePersistor, oidSequence, connectionIDFactory, gidSequenceProvider, new GroupID(1),
        stripeIdStateManager, dgcSequenceProvider);
    verify(clusterStatePersistor, never()).setGroupId(new GroupID(1));
  }

  public void testSaveNewGroupID() throws Exception {
    new ClusterState(clusterStatePersistor, oidSequence, connectionIDFactory, gidSequenceProvider, new GroupID(1),
        stripeIdStateManager, dgcSequenceProvider);
    verify(clusterStatePersistor).setGroupId(new GroupID(1));
  }
}
