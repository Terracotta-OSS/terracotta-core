/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.TextualDataFormat;

import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that provides a RESTful interface towards an embedded statistics gatherer
 */
public class StatisticsGathererServlet extends RestfulServlet {
  public static final String             GATHERER_ATTRIBUTE = StatisticsGathererServlet.class.getName() + ".gatherer";

  private L2TVSConfigurationSetupManager configSetupManager;
  private StatisticsGathererSubSystem    system;

  public void init() {
    configSetupManager = (L2TVSConfigurationSetupManager) getServletContext()
        .getAttribute(ConfigServlet.CONFIG_ATTRIBUTE);
    system = (StatisticsGathererSubSystem) getServletContext().getAttribute(GATHERER_ATTRIBUTE);
  }

  public void methodStartup(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    final NewCommonL2Config commonConfig = configSetupManager.commonl2Config();
    final NewL2DSOConfig dsoConfig = configSetupManager.dsoL2Config();
    String hostname = dsoConfig.bind().getString();
    if (null == hostname) {
      hostname = dsoConfig.host().getString();
    }
    final int port = commonConfig.jmxPort().getInt();
    system.getStatisticsGatherer().connect(hostname, port);
    printOk(response);
  }

  public void methodShutdown(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().disconnect();
    printOk(response);
  }

  public void methodReinitialize(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.reinitialize();
    printOk(response);
  }

  public void methodCreateSession(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    system.getStatisticsGatherer().createSession(sessionid);
    printOk(response);
  }

  public void methodCloseSession(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().closeSession();
    printOk(response);
  }

  public void methodGetActiveSessionId(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String sessionid = system.getStatisticsGatherer().getActiveSessionId();
    print(response, sessionid);
  }

  public void methodGetAvailableSessionIds(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String[] sessionids = system.getStatisticsStore().getAvailableSessionIds();
    print(response, sessionids);
  }

  public void methodGetAvailableAgentDifferentiators(final HttpServletRequest request,
                                                     final HttpServletResponse response) throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    String[] result = system.getStatisticsStore().getAvailableAgentDifferentiators(sessionid);
    print(response, result);
  }

  public void methodGetSupportedStatistics(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String[] statistics = system.getStatisticsGatherer().getSupportedStatistics();
    print(response, statistics);
  }

  public void methodEnableStatistics(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String[] names = request.getParameterValues("names");
    if (null == names) throw new IllegalArgumentException("names");
    system.getStatisticsGatherer().enableStatistics(names);
    printOk(response);
  }

  public void methodCaptureStatistic(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String name = request.getParameter("name");
    if (null == name) throw new IllegalArgumentException("name");
    StatisticData[] data = system.getStatisticsGatherer().captureStatistic(name);
    response.setContentType("text/plain");
    StringBuffer out = new StringBuffer();
    out.append(StatisticData.CURRENT_CSV_HEADER);
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        out.append(data[i].toCsv());
      }
    }
    print(response, out.toString());
  }

  public void methodStartCapturing(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    system.getStatisticsGatherer().startCapturing();
    printOk(response);
  }

  public void methodStopCapturing(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    system.getStatisticsGatherer().stopCapturing();
    printOk(response);
  }

  public void methodSetGlobalParam(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    system.getStatisticsGatherer().setGlobalParam(key, value);
    printOk(response);
  }

  public void methodGetGlobalParam(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = system.getStatisticsGatherer().getGlobalParam(key);
    print(response, value);
  }

  public void methodSetSessionParam(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    system.getStatisticsGatherer().setSessionParam(key, value);
    printOk(response);
  }

  public void methodGetSessionParam(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = system.getStatisticsGatherer().getSessionParam(key);
    print(response, value);
  }

  public void methodClearStatistics(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    system.getStatisticsStore().clearStatistics(sessionid);
    printOk(response);
  }

  public void methodClearAllStatistics(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    system.getStatisticsStore().clearAllStatistics();
    printOk(response);
  }

  public void methodRetrieveStatistics(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    final StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria()
        .sessionId(request.getParameter("sessionId")).agentIp(request.getParameter("agentIp"))
        .agentDifferentiator(request.getParameter("agentDifferentiator")).setNames(request.getParameterValues("names"))
        .setElements(request.getParameterValues("elements"));

    final boolean textformat = "txt".equals(request.getParameter("format"));

    DateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    String filename_base = "statistics-" + format.format(new Date());

    if (textformat) {
      response.setContentType("text/plain");
    } else {
      response.setHeader("Content-Disposition", "attachment; filename=\"" + filename_base + ".zip\"");
      response.setContentType("application/zip");
    }
    response.setStatus(HttpServletResponse.SC_OK);

    OutputStream os = response.getOutputStream();
    system.getStatisticsStore().retrieveStatisticsAsCsvStream(os, filename_base, criteria, !textformat);
  }

  public void methodAggregateStatisticsData(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);

    Long interval = null;
    String interval_string = request.getParameter("interval");
    if (interval_string != null) {
      interval = new Long(interval_string);
    }

    Writer writer = response.getWriter();
    system.getStatisticsStore().aggregateStatisticsData(writer,
                                                        TextualDataFormat.getFormat(request.getParameter("format")),
                                                        request.getParameter("sessionId"),
                                                        request.getParameter("agentDifferentiator"),
                                                        request.getParameterValues("names"),
                                                        request.getParameterValues("elements"), interval);
    writer.close();
  }
}
