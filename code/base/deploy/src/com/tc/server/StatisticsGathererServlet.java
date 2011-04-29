/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticDataCSVParser;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.gatherer.StatisticsGathererListener;
import com.tc.statistics.gatherer.exceptions.StatisticsGathererException;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.TextualDataFormat;

import java.io.OutputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that provides a RESTful interface towards an embedded statistics gatherer
 */
public class StatisticsGathererServlet extends RestfulServlet implements StatisticsGathererListener {
  private static final long                     serialVersionUID   = -8552317343856988647L;

  public static final String                    GATHERER_ATTRIBUTE = StatisticsGathererServlet.class.getName()
                                                                     + ".gatherer";

  private transient L2ConfigurationSetupManager configSetupManager;
  private transient StatisticsGathererSubSystem system;

  private boolean                               connected          = false;

  @Override
  public void init() {
    configSetupManager = (L2ConfigurationSetupManager) getServletContext().getAttribute(ConfigServlet.CONFIG_ATTRIBUTE);
    system = (StatisticsGathererSubSystem) getServletContext().getAttribute(GATHERER_ATTRIBUTE);
    system.getStatisticsGatherer().addListener(this);
  }

  public void methodStartup(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    startup();
    printOk(response);
  }

  private synchronized void startup() throws StatisticsGathererException {
    if (connected) { return; }

    final CommonL2Config commonConfig = configSetupManager.commonl2Config();
    final L2DSOConfig dsoConfig = configSetupManager.dsoL2Config();
    String hostname = configSetupManager.commonl2Config().jmxPort().getBind();
    if (null == hostname) {
      hostname = dsoConfig.host();
    }
    final int port = commonConfig.jmxPort().getIntValue();
    system.getStatisticsGatherer().connect(hostname, port);
  }

  public synchronized void methodShutdown(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    if (!connected) {
      system.getStatisticsGatherer().disconnect();
    }
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
    StringBuilder out = new StringBuilder();
    out.append(StatisticDataCSVParser.CURRENT_CSV_HEADER);
    if (data != null) {
      for (StatisticData element : data) {
        out.append(element.toCsv());
      }
    }
    print(response, out.toString());
  }

  public void methodRetrieveStatisticData(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    String[] names = request.getParameterValues("names");
    if (null == names) throw new IllegalArgumentException("names");

    response.setContentType("text/xml");
    StringBuilder out = new StringBuilder();

    out.append("<?xml version=\"1.0\"?>\n");
    out.append("<statistics>\n");

    for (String name : names) {
      out.append("  <statistic type=\"" + name + "\">\n");
      StatisticData[] data = system.getStatisticsGatherer().retrieveStatisticData(name);
      if (data != null) {
        for (StatisticData element : data) {
          out.append("    <data>\n");
          out.append("      ");
          out.append(element.toXml());
          out.append("\n");
          out.append("    </data>\n");
        }
      }
      out.append("  </statistic>\n");
    }

    out.append("</statistics>\n");

    print(response, out.toString());
  }

  private final static long                              REALTIME_DATA_INTERVAL   = 1000L;
  private final static long                              REALTIME_DATA_BUFFERSIZE = 60;
  private final static long                              REALTIME_DATA_MAXAGE     = REALTIME_DATA_BUFFERSIZE
                                                                                    * REALTIME_DATA_INTERVAL;

  private final static Pattern                           XML_TAG_NAME_PATTERN     = Pattern.compile("\\W");

  private final ReentrantReadWriteLock                   realtimeDataLock         = new ReentrantReadWriteLock();
  private final Map<String, LinkedList<StatisticData[]>> realtimeData             = new HashMap<String, LinkedList<StatisticData[]>>();
  private long                                           lastRealtimeData         = 0L;

  private String xmlTagName(final String name) {
    return XML_TAG_NAME_PATTERN.matcher(name).replaceAll("_");
  }

  public void methodRealtime(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    final String[] names = request.getParameterValues("names");
    if (null == names) throw new IllegalArgumentException("names");

    startup();

    updateRealtimeData(names);

    response.setContentType("text/xml");

    StringBuilder out = new StringBuilder();

    realtimeDataLock.readLock().lock();
    try {
      Map<String, Map<String, Map<String, Map<Date, List<StatisticData>>>>> nodes = new LinkedHashMap<String, Map<String, Map<String, Map<Date, List<StatisticData>>>>>();

      for (Map.Entry<String, LinkedList<StatisticData[]>> entry : realtimeData.entrySet()) {
        final String statName = entry.getKey();

        for (StatisticData[] statDataSnapshot : entry.getValue()) {
          for (StatisticData statData : statDataSnapshot) {

            Map<String, Map<String, Map<Date, List<StatisticData>>>> stats = nodes.get(statData
                .getAgentDifferentiator());
            if (null == stats) {
              stats = new HashMap<String, Map<String, Map<Date, List<StatisticData>>>>();
              nodes.put(statData.getAgentDifferentiator(), stats);
            }

            Map<String, Map<Date, List<StatisticData>>> elements = stats.get(statName);
            if (null == elements) {
              elements = new HashMap<String, Map<Date, List<StatisticData>>>();
              stats.put(statName, elements);
            }

            String element = statData.getElement();
            if (null == element) {
              element = statName;
            }
            Map<Date, List<StatisticData>> timeseries = elements.get(element);
            if (null == timeseries) {
              timeseries = new LinkedHashMap<Date, List<StatisticData>>();
              elements.put(element, timeseries);
            }

            List<StatisticData> dataElements = timeseries.get(statData.getMoment());
            if (null == dataElements) {
              dataElements = new ArrayList<StatisticData>();
              timeseries.put(statData.getMoment(), dataElements);
            }

            dataElements.add(statData);
          }
        }
      }

      out.append("<?xml version=\"1.0\"?>\n");
      out.append("<nodes>\n");
      for (Map.Entry<String, Map<String, Map<String, Map<Date, List<StatisticData>>>>> nodeEntry : nodes.entrySet()) {
        final String nodeName = xmlTagName(nodeEntry.getKey());
        out.append("  <");
        out.append(nodeName);
        out.append(">\n");
        for (Map.Entry<String, Map<String, Map<Date, List<StatisticData>>>> statsEntry : nodeEntry.getValue()
            .entrySet()) {
          final String statName = xmlTagName(statsEntry.getKey());
          out.append("    <");
          out.append(statName);
          out.append(">\n");
          for (Map.Entry<String, Map<Date, List<StatisticData>>> elementEntry : statsEntry.getValue().entrySet()) {
            final String elementName = xmlTagName(elementEntry.getKey());
            out.append("      <");
            out.append(elementName);
            out.append(">\n");

            Set<Map.Entry<Date, List<StatisticData>>> timeseriesSet = elementEntry.getValue().entrySet();
            if (elementEntry.getValue().size() > 1 && elementEntry.getValue().size() < REALTIME_DATA_BUFFERSIZE) {
              outputRealtimeDataElement(out, System.currentTimeMillis() - REALTIME_DATA_MAXAGE, timeseriesSet
                  .iterator().next().getValue());
            }
            for (Map.Entry<Date, List<StatisticData>> timeseriesEntry : timeseriesSet) {
              long time = timeseriesEntry.getKey().getTime();
              List<StatisticData> data = timeseriesEntry.getValue();
              outputRealtimeDataElement(out, time, data);
            }

            out.append("      </");
            out.append(elementName);
            out.append(">\n");
          }
          out.append("    </");
          out.append(statName);
          out.append(">\n");
        }
        out.append("  </");
        out.append(nodeName);
        out.append(">\n");
      }
      out.append("</nodes>");
    } finally {
      realtimeDataLock.readLock().unlock();
    }

    print(response, out.toString());
  }

  private void outputRealtimeDataElement(StringBuilder out, long time, List<StatisticData> data) {
    out.append("        <data>");
    out.append("<m>");
    out.append(time);
    out.append("</m>");
    int i = 1;
    for (StatisticData dataElement : data) {
      out.append("<v");
      out.append(i);
      out.append(">");
      out.append(dataElement.getData());
      out.append("</v");
      out.append(i);
      out.append(">");
      i++;
    }
    out.append("</data>\n");
  }

  private void updateRealtimeData(final String[] statNames) throws StatisticsGathererException {
    realtimeDataLock.readLock().lock();
    try {
      if (System.currentTimeMillis() < lastRealtimeData + REALTIME_DATA_INTERVAL) { return; }
    } finally {
      realtimeDataLock.readLock().unlock();
    }

    realtimeDataLock.writeLock().lock();
    try {
      for (String statName : statNames) {
        StatisticData[] data = system.getStatisticsGatherer().retrieveStatisticData(statName);

        LinkedList<StatisticData[]> aggregatedData = realtimeData.get(statName);
        if (null == aggregatedData) {
          aggregatedData = new LinkedList<StatisticData[]>();
          realtimeData.put(statName, aggregatedData);
        }

        // remove outdated aggregated data
        Iterator<StatisticData[]> it = aggregatedData.iterator();
        while (it.hasNext()) {
          if (it.next()[0].getMoment().getTime() + REALTIME_DATA_MAXAGE < System.currentTimeMillis()) {
            it.remove();
          }
        }

        if (data != null && data.length > 0) {
          aggregatedData.addLast(data);
          if (aggregatedData.size() > REALTIME_DATA_BUFFERSIZE) {
            aggregatedData.removeFirst();
          }
          lastRealtimeData = System.currentTimeMillis();
        }
      }
    } finally {
      realtimeDataLock.writeLock().unlock();
    }
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
      interval = Long.valueOf(interval_string);
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

  public void capturingStarted(String sessionId) {
    // nothing to do
  }

  public void capturingStopped(String sessionId) {
    // nothing to do
  }

  public synchronized void connected(String managerHostName, int managerPort) {
    connected = true;
  }

  public synchronized void disconnected() {
    connected = false;
  }

  public void reinitialized() {
    // nothing to do
  }

  public void sessionClosed(String sessionId) {
    // nothing to do
  }

  public void sessionCreated(String sessionId) {
    // nothing to do
  }

  public void statisticsEnabled(String[] names) {
    // nothing to do
  }
}
