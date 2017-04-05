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

import com.tc.entity.DiagnosticMessage;
import com.tc.entity.DiagnosticResponse;
import com.tc.exception.TCRuntimeException;
import com.tc.l2.logging.TCLoggingLog4J;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.TCLoggingService;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.logging.TCLoggingBroadcaster;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.UnsupportedMessageTypeException;
import com.tc.server.TCServer;
import com.tc.util.StringUtil;
import com.tc.util.runtime.ThreadDumpUtil;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DiagnosticsHandler implements TCMessageSink {
  
  private final TCLogger logger = TCLogging.getLogger(DiagnosticsHandler.class);
  private final DistributedObjectServer server;
  private final TCServer tcServer;
  private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
  private TCServerInfoMBean tcServerInfoMBean;
  private static DiagnosticAppender diagnosticAppender;

  // basically copied form JMXLogging.java
  static {
    //  hack to get the underlying service for logging implementation
    TCLoggingService service = TCLogging.getLoggingService();
    // all logging goes to JMX based appender
    if (service instanceof TCLoggingLog4J) {
      diagnosticAppender = new DiagnosticAppender();
      diagnosticAppender.setLayout(new PatternLayout(TCLoggingLog4J.FILE_AND_JMX_PATTERN));
      diagnosticAppender.setName("JMX appender");
      ((TCLoggingLog4J)service).addToAllLoggers(diagnosticAppender);
    }
  }

  public DiagnosticsHandler(DistributedObjectServer server, TCServer tcServer) {
    this.server = server;
    this.tcServer = tcServer;
    try {
      ObjectName internalTerracottaServerObjectName = new ObjectName("org.terracotta:name=TerracottaServer");
      tcServerInfoMBean = JMX.newMBeanProxy(mBeanServer, internalTerracottaServerObjectName, TCServerInfoMBean.class);
    } catch (JMException e) {
      System.err.println("Ouch, Diagnostic entity won't work properly because of a JMX Exception !" +  e.getMessage());
    }
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
    String cmd = new String(data, set);
    byte[] result = null;
    try {
      switch (cmd) {
        case "getState":
          result = server.getContext().getL2Coordinator().getStateManager().getCurrentState().getName().getBytes(set);
          break;
        case "getClusterState":
          result = server.getClusterState(set);
          break;
        case "getConfig":
          result = tcServer.getConfig().getBytes(set);
          break;
        case "getProcessArguments":
          result = StringUtil.toString(tcServer.processArguments(), " ", null, null).getBytes(set);
          break;
        case "getServerInfoAttributes":
          if(tcServerInfoMBean == null) {
            result = "500 KIND_OF : The tcServerInfoMBean could not be initialized.".getBytes(set);
          } else {
            StringBuilder stringBuilder =  new StringBuilder();
            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put("tcProperties", tcServerInfoMBean.getTCProperties());
            resultMap.put("config", tcServerInfoMBean.getConfig());
            resultMap.put("environment", tcServerInfoMBean.getEnvironment());
            resultMap.put("processArguments", tcServerInfoMBean.getProcessArguments());

            for (Map.Entry<String, Object> stringObjectEntry : resultMap.entrySet()) {
              stringBuilder
                  .append(stringObjectEntry.getKey())
                  .append("$KEY_VALUE_SEPARATOR$")
                  .append(stringObjectEntry.getValue())
                  .append("$ENTRY_SEPARATOR$");
            }
            result = stringBuilder.toString().getBytes(set);
          }
          break;

        case "takeThreadDump":
          result = ThreadDumpUtil.getThreadDump().getBytes(set);
          break;
        case "terminateServer":
          tcServer.shutdown();
          break;
        case "forceTerminateServer":
          Runtime.getRuntime().exit(0);
          break;
        case "getLogs":
          StringBuilder stringBuilder =  new StringBuilder();
          List<Notification> logNotifications = diagnosticAppender.getBroadcastingBean().getLogNotifications();
          for (Notification logNotification : logNotifications) {
            stringBuilder.append(logNotification.getMessage());
          }
          result = stringBuilder.toString().getBytes(set);
          break;

        default:
          result = "UNKNOWN CMD".getBytes(set);
          break;
      }
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      resp.setResponse(msg.getTransactionID(), result);
      resp.send();
    } catch (RuntimeException runtime) {
      logger.warn("diagnostic", runtime);
      DiagnosticResponse resp = (DiagnosticResponse)channel.createMessage(TCMessageType.DIAGNOSTIC_RESPONSE);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(out, set);
      PrintWriter pw = new PrintWriter(writer);
      runtime.printStackTrace(pw);
      resp.setResponse(msg.getTransactionID(), out.toByteArray());
      resp.send();
    }
  }

  // basically copied from JMXAppender.java
  private static class DiagnosticAppender extends AppenderSkeleton {

    private final TCLoggingBroadcaster broadcastingBean;

    public DiagnosticAppender() {
      try {
        broadcastingBean = new TCLoggingBroadcaster();
      } catch (NotCompliantMBeanException ncmbe) {
        throw new TCRuntimeException("Unable to construct the broadcasting bean: this is a programming error in "
            + TCLoggingBroadcaster.class.getName(), ncmbe);
      }
    }

    public final TCLoggingBroadcaster getBroadcastingBean() {
      return broadcastingBean;
    }

    @Override
    protected void append(LoggingEvent event) {
      broadcastingBean.broadcastLogEvent(getLayout().format(event), event.getThrowableStrRep());
    }

    @Override
    public boolean requiresLayout() {
      return false;
    }

    @Override
    public void close() {
      // Do nothing
    }

  }
}
