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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.entity.DiagnosticMessage;
import com.tc.entity.DiagnosticResponse;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.server.TCServerMain;
import com.tc.util.StringUtil;
import com.tc.util.runtime.ThreadDumpUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 *
 */
public class DiagnosticsHandler implements TCMessageSink {
  
  private final static Logger logger = LoggerFactory.getLogger(DiagnosticsHandler.class);
  private final DistributedObjectServer server;
  private final JMXSubsystem subsystem;

  public DiagnosticsHandler(DistributedObjectServer server) {
    this.server = server;
    this.subsystem = new JMXSubsystem();
  }
  
  @Override
  public void putMessage(TCMessage message) throws UnsupportedMessageTypeException {
    Charset set = Charset.forName("UTF-8");
    MessageChannel channel = message.getChannel();
    try {
      message.hydrate();
    } catch (Exception e) {
      logger.warn("trouble with diagnostics", e);
    }
    DiagnosticMessage msg = (DiagnosticMessage)message;
    byte[] data = msg.getExtendedData();
    String raw = new String(data, set);
    String[] cmd = raw.split(" ");
    byte[] result = null;
    try {
      switch (cmd[0]) {
        case "getState":
          result = server.getContext().getL2Coordinator().getStateManager().getCurrentMode().getName().getBytes(set);
          break;
        case "getClusterState":
          result = server.getClusterState(set);
          break;
        case "getConfig":
          result = TCServerMain.getServer().getConfig().getBytes(set);
          break;
        case "getProcessArguments":
          result = StringUtil.toString(TCServerMain.getServer().processArguments(), " ", null, null).getBytes(set);
          break;
        case "getThreadDump":
          result = ThreadDumpUtil.getThreadDump().getBytes(set);
          break;
        case "terminateServer":
          TCServerMain.getServer().shutdown();
          // never used, server is dead
          result = "".getBytes(set);
          break;
        case "forceTerminateServer":
          Runtime.getRuntime().exit(0);
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
          if (cmd.length != 3) {
            result = ("Invalid JMX call:" + raw).getBytes(set);
          } else {
            result = subsystem.call(cmd[1], cmd[2], null).getBytes(set);
          }
          break;
        case "invokeWithArgJMX":
          if (cmd.length != 4) {
            result = ("Invalid JMX call:" + raw).getBytes(set);
          } else {
            result = subsystem.call(cmd[1], cmd[2], cmd[3]).getBytes(set);
          }
          break;
        default:
          result = "UNKNOWN CMD".getBytes(set);
          break;
      }
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      resp.setResponse(msg.getTransactionID(), result);
      resp.send();
    } catch (Throwable t) {
      logger.warn("caught exception while running diagnostic command: " + Arrays.toString(cmd), t);
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(out, set);
      PrintWriter pw = new PrintWriter(writer);
      t.printStackTrace(pw);
      resp.setResponse(msg.getTransactionID(), out.toByteArray());
      resp.send();
    }
  }
}
