/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class structures the data that is retrieved through a {@link StatisticRetrievalAction}. New instances are
 * usually filled in through the specialized constructors. The {@code agentIp} and {@code agentDifferentiator}
 * properties don't have to be filled in. When an instance of {@code StatisticData} is stored in the {@code
 * StatisticsBuffer} of the CVT agent, these properties are set when they are {@code null}. This should be the desired
 * behavior is almost all of the cases. Also, the {@code moment} property will be filled in by the {@code
 * StatisticsRetriever} to ensure that all the data in one retrieval action can be correlated. Only four types of data
 * can be stored within a {@code StatisticData} instance and they are mutually exclusive. The reason why it doesn't
 * allow any {@code Object} to be used as data is to allow for the CVT back-end to store the data while preserving its
 * type. This makes is easier to query on the data values after collection. The {@code name} of a {@code StatisticData}
 * instance should identify the type of data that it contains, for instance "{@code cpu combined}". The {@code element}
 * property can be {@code null}, but when it's used it should identify different elements of the same data. For instance
 * when CPU data is collected for multiple CPUs, the names are the same, but the elements will be "{@code cpu 1}", "
 * {@code cpu 2}", ... so that it's possible to identify the individual data points. Finally, when
 * {@link StatisticRetrievalAction}s return an array of {@code StatisticData} instances, the {@code moment} property of
 * each individual data instance should be the same so that the entire array can be situated at the same location on a
 * timeline. Usually this is done by creating a {@link Date} instance before instantiating the {@code StatisticData}
 * instances and passing that {@code Date} instance to the constructor of each data element.
 */
public class StatisticData implements Serializable {
  public final static StatisticData[] EMPTY_ARRAY      = new StatisticData[0];

  private final static long           serialVersionUID = -3387790670840965825L;

  private String                      sessionId;
  private String                      agentIp;
  private String                      agentDifferentiator;
  private Date                        moment;
  private String                      name;
  private String                      element;
  private Object                      data;

  public StatisticData() {
    //
  }

  public StatisticData(final String name, final Long value) {
    setName(name);
    setData(value);
  }

  public StatisticData(final String name, final String value) {
    setName(name);
    setData(value);
  }

  public StatisticData(final String name, final Date value) {
    setName(name);
    setData(value);
  }

  public StatisticData(final String name, final BigDecimal value) {
    setName(name);
    setData(value);
  }

  public StatisticData(final String name, final String element, final Long value) {
    setName(name);
    setElement(element);
    setData(value);
  }

  public StatisticData(final String name, final String element, final String value) {
    setName(name);
    setElement(element);
    setData(value);
  }

  public StatisticData(final String name, final String element, final Date value) {
    setName(name);
    setElement(element);
    setData(value);
  }

  public StatisticData(final String name, final String element, final BigDecimal value) {
    setName(name);
    setElement(element);
    setData(value);
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(final String sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticData sessionId(final String sessionID) {
    setSessionId(sessionID);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public void setAgentIp(final String agentIp) {
    this.agentIp = agentIp;
  }

  public StatisticData agentIp(final String agentIP) {
    setAgentIp(agentIP);
    return this;
  }

  public String getAgentDifferentiator() {
    return agentDifferentiator;
  }

  public StatisticData agentDifferentiator(final String agentDiff) {
    setAgentDifferentiator(agentDiff);
    return this;
  }

  public void setAgentDifferentiator(final String agentDifferentiator) {
    this.agentDifferentiator = agentDifferentiator;
  }

  public void setMoment(final Date moment) {
    this.moment = moment;
  }

  public StatisticData moment(final Date date) {
    setMoment(date);
    return this;
  }

  public Date getMoment() {
    return moment;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public StatisticData name(final String nameArg) {
    setName(nameArg);
    return this;
  }

  public String getElement() {
    return element;
  }

  public void setElement(final String element) {
    this.element = element;
  }

  public StatisticData element(final String elementArg) {
    setElement(elementArg);
    return this;
  }

  public Object getData() {
    return data;
  }

  private void setData(final Object data) {
    this.data = data;
  }

  private StatisticData data(final Object obj) {
    setData(obj);
    return this;
  }

  public void setData(final Long data) {
    setData((Object) data);
  }

  public StatisticData data(final Long longData) {
    return data((Object) longData);
  }

  public void setData(final String data) {
    setData((Object) data);
  }

  public StatisticData data(final String strData) {
    return data((Object) strData);
  }

  public void setData(final Date data) {
    setData((Object) data);
  }

  public StatisticData data(final Date dateData) {
    return data((Object) dateData);
  }

  public void setData(final BigDecimal data) {
    setData((Object) data);
  }

  public StatisticData data(final BigDecimal bigDecimalData) {
    return data((Object) bigDecimalData);
  }

  @Override
  public Object clone() {
    return new StatisticData().sessionId(sessionId).agentIp(agentIp).agentDifferentiator(agentDifferentiator)
        .moment(moment).name(name).element(element).data(data);
  }

  @Override
  public String toString() {
    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
    String data_formatted;
    if (data != null && data instanceof Date) {
      data_formatted = format.format(data);
    } else {
      data_formatted = String.valueOf(data);
    }
    return "[" + "sessionId = " + sessionId + "; " + "agentIp = " + agentIp + "; " + "agentDifferentiator = "
           + agentDifferentiator + "; " + "moment = "
           + (null == moment ? String.valueOf(moment) : format.format(moment)) + "; " + "name = " + name + "; "
           + "element = " + element + "; " + "data = " + data_formatted + "" + "]";
  }

  public String toLog() {
    String data_formatted;
    if (data != null && data instanceof Date) {
      DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
      data_formatted = format.format(data);
    } else {
      data_formatted = String.valueOf(data);
    }
    return name + (null == element ? "" : " - " + element) + " : " + data_formatted;
  }

  public static String escapeForCsv(final String value) {
    char[] chars = value.toCharArray();
    StringBuffer buffer = null;
    int last = 0;
    String replacement = null;
    for (int i = 0; i < chars.length; i++) {
      switch (chars[i]) {
        case '\\':
          replacement = "\\\\";
          break;
        case '"':
          replacement = "\\\"";
          break;
        case '\r':
          replacement = "";
          break;
        case '\n':
          replacement = "\\n";
          break;
      }

      if (replacement != null) {
        if (null == buffer) {
          buffer = new StringBuffer();
        }

        if (last < i) {
          buffer.append(chars, last, i - last);
        }

        buffer.append(replacement);
        replacement = null;
        last = i + 1;
      }
    }

    if (null == buffer) { return value; }

    if (last < value.length()) {
      buffer.append(chars, last, value.length() - last);
    }

    return buffer.toString();
  }

  private static void addCsvField(final StringBuffer result, final Object field, final boolean separator) {
    if (null == field) {
      if (separator) {
        result.append(",");
      }
    } else {
      result.append("\"");
      result.append(escapeForCsv(String.valueOf(field)));
      result.append("\"");
      if (separator) {
        result.append(",");
      }
    }
  }

  /**
   * Converts this data instance to a single line of CSV text, terminated by a new line character. All fields are
   * separated by commas and are delimited by double quotes. Double quotes, back slashes, and new lines are escaped by a
   * back slash. Carriage returns are stripped away.
   * 
   * @return the CSV text of this data instance
   */
  public String toCsv() {
    StringBuffer result = new StringBuffer();
    addCsvField(result, sessionId, true);
    addCsvField(result, agentIp, true);
    addCsvField(result, agentDifferentiator, true);
    addCsvField(result, (null == moment ? null : new Long((moment).getTime())), true);
    addCsvField(result, name, true);
    addCsvField(result, element, true);
    if (null == data) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof BigDecimal) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, data, false);
    } else if (data instanceof Number) {
      addCsvField(result, data, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof CharSequence) {
      addCsvField(result, null, true);
      addCsvField(result, data, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof Date) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, (null == data ? null : new Long(((Date) data).getTime())), true);
      addCsvField(result, null, false);
    }
    result.append("\n");
    return result.toString();
  }

  /**
   * Converts this data instance to an XML representation. This only outputs the values of the fields as XML tags,
   * there's no surrounding &lt;statistic&gt; tag.
   * 
   * @return the XML representation of this data
   */
  public String toXml() {
    StringBuffer result = new StringBuffer();
    result.append("<sessionId>");
    if (sessionId != null) {
      result.append(sessionId);
    }
    result.append("</sessionId>");

    result.append("<agentIp>");
    if (agentIp != null) {
      result.append(agentIp);
    }
    result.append("</agentIp>");

    result.append("<agentDifferentiator>");
    if (agentDifferentiator != null) {
      result.append(agentDifferentiator);
    }
    result.append("</agentDifferentiator>");

    result.append("<moment>");
    if (moment != null) {
      result.append(new Long((moment).getTime()));
    }
    result.append("</moment>");

    result.append("<name>");
    if (name != null) {
      result.append(name);
    }
    result.append("</name>");

    result.append("<element>");
    if (element != null) {
      result.append(element);
    }
    result.append("</element>");

    result.append("<value>");
    if (data != null) {
      if (data instanceof Date) {
        result.append(new Long(((Date) data).getTime()));
      } else {
        result.append(data);
      }
    }
    result.append("</value>");
    return result.toString();
  }

}