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
package com.tc.objectserver.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.entity.DiagnosticMessage;
import com.tc.entity.DiagnosticResponse;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.objectserver.core.impl.GuardianContext;
import com.tc.spi.Guardian;
import com.tc.util.State;
import com.tc.util.StringUtil;
import com.tc.util.runtime.ThreadDumpUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.StopAction;
import com.tc.net.protocol.tcm.TCAction;

/**
 *
 */
public class DiagnosticsHandler extends AbstractEventHandler<TCAction> {
  
  private final static Logger logger = LoggerFactory.getLogger(DiagnosticsHandler.class);
  private final DistributedObjectServer server;
  private final JMXSubsystem subsystem;

  public DiagnosticsHandler(DistributedObjectServer server, JMXSubsystem subsystem) {
    this.server = server;
    this.subsystem = subsystem;
  }
  
  @Override
  public void handleEvent(TCAction context) throws EventHandlerException {
    processMessage(context);
  }
  
  private void processMessage(TCAction message) {
    Charset set = Charset.forName("UTF-8");
    MessageChannel channel = message.getChannel();
    try {
      message.hydrate();
    } catch (Exception e) {
      logger.warn("trouble with diagnostics", e);
      return;
    }
    DiagnosticMessage msg = (DiagnosticMessage)message;
    TCByteBuffer data = msg.getExtendedData();
    String raw = new String(TCByteBufferFactory.unwrap(data), set);
    String[] cmd = raw.split(" ");
    byte[] result = null;
    long startTime = System.currentTimeMillis();
    ChannelID channelID = message.getChannel().getChannelID();
    try {
      GuardianContext.setCurrentChannelID(channelID);
      switch (cmd[0]) {
        case "getState":
          result = server.getContext().getL2Coordinator().getStateManager().getCurrentMode().getName().getBytes(set);
          break;
        case "getInitialState":
          State initialState = server.getPersistor().getClusterStatePersistor().getInitialState();
          result = initialState != null ? initialState.getName().getBytes(set) : "".getBytes(set);
          break;
        case "getClusterState":
          if (GuardianContext.validate(Guardian.Op.SERVER_DUMP, "getClusterState")) {
            result = server.getClusterState(set, null);
          } else {
            result = "NOT PERMITTED".getBytes(set);
          }
          break;
        case "getConfig":
          result = ServerEnv.getServer().getConfiguration().getBytes(set);
          break;
        case "getProcessArguments":
          result = StringUtil.toString(ServerEnv.getServer().processArguments(), " ", null, null).getBytes(set);
          break;
        case "getThreadDump":
          result = ThreadDumpUtil.getThreadDump().getBytes(set);
          break;
        case "terminateServer":
          ServerEnv.getServer().stop();
          // never used, server is dead
          result = "".getBytes(set);
          break;
        case "restartServer":
          ServerEnv.getServer().stop(StopAction.RESTART);
          // never used, server is dead
          result = "".getBytes(set);
          break;
        case "forceTerminateServer":
          ServerEnv.getServer().stop();
          // never used, server is dead
          result = "".getBytes(set);
          break;
        case "getJMX":
          if (cmd.length != 3) {
            result = ("Invalid JMX get:" + raw).getBytes(set);
          } else {
            result = subsystem.get(cmd[1], cmd[2]).getBytes(set);
          }
          break;
        case "setJMX":
          if (cmd.length != 4) {
            result = ("Invalid JMX set:" + raw).getBytes(set);
          } else {
            result = subsystem.set(cmd[1], cmd[2], cmd[3]).getBytes(set);
          }
          break;
        case "invokeJMX":
          if (cmd.length < 3) {
            result = ("Invalid JMX call:" + raw).getBytes(set);
          } else if (cmd.length == 3) {
            GuardianContext.validate(Guardian.Op.GENERIC_OP, cmd[2]);
            result = subsystem.call(cmd[1], cmd[2]).getBytes(set);
          } else {
            GuardianContext.validate(Guardian.Op.GENERIC_OP, cmd[2]);
            result = subsystem.call(cmd[1], cmd[2], Arrays.copyOfRange(cmd, 3, cmd.length)).getBytes(set);
          }
          break;
        case "invokeWithArgJMX":
          if (cmd.length != 4) {
            result = ("Invalid JMX call:" + raw).getBytes(set);
          } else {
            result = subsystem.call(cmd[1], cmd[2], cmd[3]).getBytes(set);
          }
          break;
        case "list":
          result = subsystem.info(cmd[1]).getBytes(set);
          break;
        default:
          result = "UNKNOWN CMD".getBytes(set);
          break;
      }
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      resp.setResponse(msg.getTransactionID(), result);
      resp.send();
      long end = System.currentTimeMillis();
      if (end - startTime > 500) {
        logger.warn("command {} took {}ms", raw, end - startTime);
      }
      logger.debug("command {} took {}ms and returned {}", raw, end - startTime, new String(result, set));
    } catch (Throwable t) {
      logger.warn("caught exception while running diagnostic command: " + Arrays.toString(cmd), t);
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(out, set);
      PrintWriter pw = new PrintWriter(writer);
      t.printStackTrace(pw);
      resp.setResponse(msg.getTransactionID(), out.toByteArray());
      resp.send();
    } finally {
      GuardianContext.clearCurrentChannelID(channelID);
    }
  }
}
