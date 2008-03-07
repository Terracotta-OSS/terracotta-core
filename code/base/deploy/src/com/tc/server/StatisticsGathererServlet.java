/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.net.TCSocketAddress;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.store.StatisticDataUser;
import com.tc.statistics.store.StatisticsRetrievalCriteria;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that provides a RESTful interface towards an embedded statistics gatherer
 */
public class StatisticsGathererServlet extends RestfulServlet {
  public static final String GATHERER_ATTRIBUTE = StatisticsGathererServlet.class.getName() + ".gatherer";

  private L2TVSConfigurationSetupManager configSetupManager;
  private StatisticsGathererSubSystem    system;

  public void init() {
    configSetupManager = (L2TVSConfigurationSetupManager)getServletContext().getAttribute(ConfigServlet.CONFIG_ATTRIBUTE);
    system = (StatisticsGathererSubSystem)getServletContext().getAttribute(GATHERER_ATTRIBUTE);
  }

  public void methodConnect(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().connect(TCSocketAddress.LOOPBACK_IP, configSetupManager.commonl2Config().jmxPort().getInt());
    printOk(response);
  }

  public void methodDisconnect(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().disconnect();
    printOk(response);
  }

  public void methodReinitialize(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.reinitialize();
    printOk(response);
  }

  public void methodCreateSession(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    system.getStatisticsGatherer().createSession(sessionid);
    printOk(response);
  }

  public void methodCloseSession(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().closeSession();
    printOk(response);
  }

  public void methodGetActiveSessionId(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String sessionid = system.getStatisticsGatherer().getActiveSessionId();
    print(response, sessionid);
  }

  public void methodGetAvailableSessionIds(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] sessionids = system.getStatisticsStore().getAvailableSessionIds();
    print(response, sessionids);
  }

  public void methodGetSupportedStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] statistics = system.getStatisticsGatherer().getSupportedStatistics();
    print(response, statistics);
  }

  public void methodEnableStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String[] names = request.getParameterValues("names");
    if (null == names) throw new IllegalArgumentException("names");
    system.getStatisticsGatherer().enableStatistics(names);
    printOk(response);
  }

  public void methodCaptureStatistic(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
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

  public void methodStartCapturing(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().startCapturing();
    printOk(response);
  }

  public void methodStopCapturing(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    system.getStatisticsGatherer().stopCapturing();
    printOk(response);
  }

  public void methodSetGlobalParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    system.getStatisticsGatherer().setGlobalParam(key, value);
    printOk(response);
  }

  public void methodGetGlobalParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = system.getStatisticsGatherer().getGlobalParam(key);
    print(response, value);
  }

  public void methodSetSessionParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    String value = request.getParameter("value");
    if (null == key) throw new IllegalArgumentException("key");
    if (null == value) throw new IllegalArgumentException("value");
    system.getStatisticsGatherer().setSessionParam(key, value);
    printOk(response);
  }

  public void methodGetSessionParam(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String key = request.getParameter("key");
    if (null == key) throw new IllegalArgumentException("key");
    Object value = system.getStatisticsGatherer().getSessionParam(key);
    print(response, value);
  }

  public void methodClearStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    String sessionid = request.getParameter("sessionId");
    if (null == sessionid) throw new IllegalArgumentException("sessionId");
    system.getStatisticsStore().clearStatistics(sessionid);
    printOk(response);
  }

  public void methodRetrieveStatistics(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    final StatisticsRetrievalCriteria criteria = new StatisticsRetrievalCriteria()
      .sessionId(request.getParameter("sessionId"))
      .agentIp(request.getParameter("agentIp"))
      .agentDifferentiator(request.getParameter("agentDifferentiator"))
      .setNames(request.getParameterValues("names"))
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
    final OutputStream out;

    try {
      final ZipOutputStream zipstream;
      if (textformat) {
        zipstream = null;
        out = os;
      } else {
        zipstream = new ZipOutputStream(os);
        zipstream.setLevel(9);
        zipstream.setMethod(ZipOutputStream.DEFLATED);
        out = zipstream;
      }

      try {
        if (zipstream != null) {
          final ZipEntry zipentry = new ZipEntry(filename_base + ".csv");
          zipentry.setComment(StatisticData.CURRENT_CSV_VERSION);
          zipstream.putNextEntry(zipentry);
        }

        try {
          out.write(StatisticData.CURRENT_CSV_HEADER.getBytes("UTF-8"));
          system.getStatisticsStore().retrieveStatistics(criteria, new StatisticDataUser() {
            public boolean useStatisticData(final StatisticData data) {
              try {
                out.write(data.toCsv().getBytes("UTF-8"));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
              return true;
            }
          });
        } finally {
          if (zipstream != null) {
            zipstream.closeEntry();
          }
        }
      } finally {
        if (zipstream != null) {
          zipstream.close();
        }
      }
    } finally {
      os.close();
    }
  }
}