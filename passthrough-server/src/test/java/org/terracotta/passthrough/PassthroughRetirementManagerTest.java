package org.terracotta.passthrough;

import org.junit.Test;
import org.terracotta.entity.EntityMessage;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class PassthroughRetirementManagerTest {

  private final PassthroughRetirementManager manager = new PassthroughRetirementManager();

  @Test
  public void testDeferCurrentMessage() throws Exception {
    PassthroughConnection connection = mock(PassthroughConnection.class);
    EntityMessage blockingMessage = mock(EntityMessage.class);

    manager.deferCurrentMessage(blockingMessage);

    //inform manager that current message (deferred one) is completed
    manager.addRetirementTuple(new PassthroughRetirementManager.RetirementTuple(connection, new byte[0]));
    List<PassthroughRetirementManager.RetirementTuple> retirementTuples =
        manager.retireableListAfterMessageDone(null);
    assertThat(retirementTuples, is(empty()));

    //inform manager that blocking message is completed
    manager.addRetirementTuple(new PassthroughRetirementManager.RetirementTuple(connection, new byte[0]));
    retirementTuples =
        manager.retireableListAfterMessageDone(blockingMessage);
    assertThat(retirementTuples.size(), is(2));
  }

  @Test
  public void testDeferCurrentMessageOnMultipleMessages() throws Exception {
    EntityMessage blockingMessage1 = mock(EntityMessage.class);
    EntityMessage blockingMessage2 = mock(EntityMessage.class);
    PassthroughConnection connection = mock(PassthroughConnection.class);

    manager.deferCurrentMessage(blockingMessage1);
    manager.deferCurrentMessage(blockingMessage2);

    //inform manager that current message (deferred one) is completed
    manager.addRetirementTuple(new PassthroughRetirementManager.RetirementTuple(connection, new byte[0]));
    List<PassthroughRetirementManager.RetirementTuple> retirementTuples =
        manager.retireableListAfterMessageDone(null);
    assertThat(retirementTuples, is(empty()));

    //inform manager that first blocking message is completed
    manager.addRetirementTuple(new PassthroughRetirementManager.RetirementTuple(connection, new byte[0]));
    retirementTuples =
        manager.retireableListAfterMessageDone(blockingMessage1);
    assertThat(retirementTuples, is(empty()));

    //inform manager that second blocking message is completed
    manager.addRetirementTuple(new PassthroughRetirementManager.RetirementTuple(connection, new byte[0]));
    retirementTuples =
        manager.retireableListAfterMessageDone(blockingMessage2);
    assertThat(retirementTuples.size(), is(3));
  }
}