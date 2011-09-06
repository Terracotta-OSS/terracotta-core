/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticType;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SRAMessagesTest extends TCTestCase {

  private MessageMonitor messageMonitor;

  @Override
  protected void setUp() throws Exception {
    messageMonitor = new MessageMonitorImpl();
  }

  @Override
  protected void tearDown() throws Exception {
    messageMonitor = null;
  }

  public void testSRAMessages() {
    SRAMessages messages = new SRAMessages(messageMonitor);
    Assert.assertEquals(StatisticType.SNAPSHOT, messages.getType());

    int[] incSizes = { 100, 120, 300, 245 }; // just some random sizes
    int[] outSizes = { 50, 125, 560, 345, 20, 456 }; // just some random sizes

    // just some random types
    TCMessageType[] msgTypes = { TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE,
        TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, TCMessageType.BENCH_MESSAGE,
        TCMessageType.BROADCAST_TRANSACTION_MESSAGE, TCMessageType.CLIENT_HANDSHAKE_MESSAGE };

    for (int i = 0; i < msgTypes.length; i++) {
      for (int j = 0; j < incSizes.length; j++) {
        messageMonitor.newIncomingMessage(new TestTCMessage(msgTypes[i], incSizes[j]));
      }
      for (int j = 0; j < outSizes.length; j++) {
        messageMonitor.newOutgoingMessage(new TestTCMessage(msgTypes[i], outSizes[j]));
      }
    }

    StatisticData[] data = messages.retrieveStatisticData();
    for (int i = 0; i < data.length; i++) {
      StatisticData statisticData = data[i];
      System.out.println(statisticData);
    }

    int totalIncSize = 0;
    for (int i = 0; i < incSizes.length; i++) {
      totalIncSize += incSizes[i];
    }
    int totalOutSize = 0;
    for (int i = 0; i < outSizes.length; i++) {
      totalOutSize += outSizes[i];
    }
    assertEquals(4 * msgTypes.length, data.length);

    Map<String, List<SRAMessagesData>> msgs = new HashMap<String, List<SRAMessagesData>>();

    for (StatisticData statisticData : data) {
      assertData(statisticData);
      SRAMessagesData msgData = SRAMessagesData.createSRAMessagesData(statisticData);
      List<SRAMessagesData> list = msgs.get(msgData.getTypeName());
      if (list == null) {
        list = new ArrayList<SRAMessagesData>();
      }
      list.add(msgData);
      msgs.put(msgData.getTypeName(), list);
    }

    assertEquals(msgTypes.length, msgs.size());

    for (List<SRAMessagesData> sraMessagesDatas : msgs.values()) {
      for (SRAMessagesData sraMessagesData : sraMessagesDatas) {
        long count = sraMessagesData.getData();
        if (sraMessagesData.getElementName().equals(SRAMessages.ELEMENT_INCOMING_COUNT)) {
          assertEquals(incSizes.length, count);
        } else if (sraMessagesData.getElementName().equals(SRAMessages.ELEMENT_INCOMING_DATA)) {
          assertEquals(totalIncSize, count);
        } else if (sraMessagesData.getElementName().equals(SRAMessages.ELEMENT_OUTGOING_COUNT)) {
          assertEquals(outSizes.length, count);
        } else if (sraMessagesData.getElementName().equals(SRAMessages.ELEMENT_OUTGOING_DATA)) {
          assertEquals(totalOutSize, count);
        }
      }
    }

  }

  private void assertData(final StatisticData statisticData) {
    assertEquals(statisticData.getName(), SRAMessages.ACTION_NAME);
    assertNull(statisticData.getAgentIp());
    assertNull(statisticData.getAgentDifferentiator());
    assertNull(statisticData.getSessionId());
  }

  private static class TestTCMessage implements TCMessage {
    private final TCMessageType type;
    private final int           length;

    private TestTCMessage(final TCMessageType type, final int length) {
      this.type = type;
      this.length = length;
    }

    public TCMessageType getMessageType() {
      return type;
    }

    public int getTotalLength() {
      return length;
    }

    public void hydrate() {
      return;
    }

    public void dehydrate() {
      return;
    }

    public void send() {
      return;
    }

    public MessageChannel getChannel() {
      return null;
    }

    public SessionID getLocalSessionID() {
      throw new ImplementMe();
    }

    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    public NodeID getDestinationNodeID() {
      throw new ImplementMe();
    }
  }

  private static class SRAMessagesData {
    private final String typeName;
    private final String elementName;

    private final long   data;

    private SRAMessagesData(final StatisticData data) {
      String element = data.getElement();
      String[] toks = element.split(SRAMessages.ELEMENT_NAME_DELIMITER);

      assertEquals(2, toks.length);

      this.typeName = toks[0];
      this.elementName = toks[1];

      this.data = (Long) data.getData();
    }

    private static SRAMessagesData createSRAMessagesData(final StatisticData data) {
      return new SRAMessagesData(data);
    }

    public String getTypeName() {
      return typeName;
    }

    public String getElementName() {
      return elementName;
    }

    public long getData() {
      return data;
    }
  }
}
