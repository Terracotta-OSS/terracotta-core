/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.util.Assert;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StatisticData implements Serializable {
  public final static StatisticData[] EMPTY_ARRAY = new StatisticData[0];

  public final static String CURRENT_CSV_VERSION = "1.0";
  public final static String CURRENT_CSV_HEADER = "Session ID,IP,Differentiator,Moment,Name,Element,Data Number,Data Text,Data Date,Data Decimal\n";

  private final static long serialVersionUID = -3387790670840965825L;

  private String sessionId;
  private String agentIp;
  private String agentDifferentiator;
  private Date moment;
  private String name;
  private String element;
  private Object data;

  public StatisticData() {
  }
  
  public StatisticData(String name, Date moment, Long value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, String value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, Date value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, BigDecimal value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, Long value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, String value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, Date value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, BigDecimal value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticData sessionId(String sessionId) {
    setSessionId(sessionId);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public void setAgentIp(String agentIp) {
    this.agentIp = agentIp;
  }

  public StatisticData agentIp(String agentIp) {
    setAgentIp(agentIp);
    return this;
  }

  public String getAgentDifferentiator() {
    return agentDifferentiator;
  }

  public StatisticData agentDifferentiator(String agentDifferentiator) {
    setAgentDifferentiator(agentDifferentiator);
    return this;
  }

  public void setAgentDifferentiator(String agentDifferentiator) {
    this.agentDifferentiator = agentDifferentiator;
  }

  public void setMoment(Date moment) {
    this.moment = moment;
  }

  public StatisticData moment(Date moment) {
    setMoment(moment);
    return this;
  }

  public Date getMoment() {
    return moment;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StatisticData name(String name) {
    setName(name);
    return this;
  }

  public String getElement() {
    return element;
  }

  public void setElement(String element) {
    this.element = element;
  }

  public StatisticData element(String element) {
    setElement(element);
    return this;
  }

  public Object getData() {
    return data;
  }

  private void setData(Object data) {
    this.data = data;
  }

  private StatisticData data(Object data) {
    setData(data);
    return this;
  }

  public void setData(Long data) {
    setData((Object)data);
  }

  public StatisticData data(Long data) {
    return data((Object)data);
  }

  public void setData(String data) {
    setData((Object)data);
  }

  public StatisticData data(String data) {
    return data((Object)data);
  }

  public void setData(Date data) {
    setData((Object)data);
  }

  public StatisticData data(Date data) {
    return data((Object)data);
  }

  public void setData(BigDecimal data) {
    setData((Object)data);
  }

  public StatisticData data(BigDecimal data) {
    return data((Object)data);
  }

  public Object clone() {
    return new StatisticData()
      .sessionId(sessionId)
      .agentIp(agentIp)
      .agentDifferentiator(agentDifferentiator)
      .moment(moment)
      .name(name)
      .element(element)
      .data(data);
  }

  public String toString() {
    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
    String data_formatted;
    if (data != null &&
        data instanceof Date) {
      data_formatted = format.format(data);
    } else {
      data_formatted = String.valueOf(data);
    }
    return "["
           + "sessionId = " + sessionId + "; "
           + "agentIp = " + agentIp + "; "
           + "agentDifferentiator = " + agentDifferentiator + "; "
           + "moment = " + (null == moment ? String.valueOf(moment): format.format(moment)) + "; "
           + "name = " + name + "; "
           + "element = " + element + "; "
           + "data = " + data_formatted + ""
           + "]";
  }

  private static String escapeForCsv(final String value) {
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
        last = i+1;
      }
    }

    if (null == buffer) {
      return value;
    }

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
      addCsvField(result, (null == data ? null : new Long(((Date)data).getTime())), true);
      addCsvField(result, null, false);
    }
    result.append("\n");
    return result.toString();
  }

  public static StatisticData newInstanceFromCsvLine(final String dataFormatVersion, final String line) throws ParseException {
    Assert.assertNotNull("dataFormatVersion", dataFormatVersion);

    if (CURRENT_CSV_VERSION.equals(dataFormatVersion)) {
      return new StatisticDataCSVParser(line).parse();
    } else {
      throw new ParseException("The data format version '" + dataFormatVersion + "' is not supported.", 0);
    }
  }

  private static class StatisticDataCSVParser {
    private final String line;
    private int position = 0;

    public StatisticDataCSVParser(final String line) {
      this.line = line;
    }

    private char getNextChar() {
      if (position == line.length()) {
        return 0;
      }
      return line.charAt(position++);
    }

    private void setField(final StatisticData data, final int position, final String value) throws ParseException {
      if (null == value) {
        return;
      }
      switch (position) {
        case 0:
          data.setSessionId(value);
          break;
        case 1:
          data.setAgentIp(value);
          break;
        case 2:
          data.setAgentDifferentiator(value);
          break;
        case 3:
          data.setMoment(new Date(Long.parseLong(value)));
          break;
        case 4:
          data.setName(value);
          break;
        case 5:
          data.setElement(value);
          break;
        case 6:
          data.setData(new Long(Long.parseLong(value)));
          break;
        case 7:
          data.setData(value);
          break;
        case 8:
          data.setData(new Date(Long.parseLong(value)));
          break;
        case 9:
          data.setData(new BigDecimal(value));
          break;
      }
    }

    public StatisticData parse() throws ParseException {
      final StatisticData data = new StatisticData();

      int field_count = 0;
      String field = null;
      // separates into fields
      fieldloop:
      while (true) {
        char ch = getNextChar();
        switch (ch) {
          case 0:
          case '\n':
          case '\r':
            setField(data, field_count, field);
            field = null;
            break fieldloop;
          case ' ':
          case '\t':
            continue;
          case ',':
            setField(data, field_count, field);
            field = null;
            field_count++;
            break;
          case '"':
            final StringBuffer buffer = new StringBuffer();
            synchronized (buffer) {
              // retrieve the value of a single field
              valueloop:
              while (true) {
                ch = getNextChar();
                switch (ch) {
                  case '\\':
                    ch = getNextChar();
                    switch (ch) {
                      case 'n':
                        buffer.append('\n');
                        break;
                      case '"':
                      case '\\':
                        buffer.append(ch);
                        break;
                    }
                    break;
                  case '"':
                    break valueloop;
                  case 0:
                  case '\n':
                  case '\r':
                    throw new ParseException("Unexpected line ending.", position);
                  default:
                    buffer.append(ch);
                    break;
                }
              }

              field = buffer.toString();
              break;
            }
          default:
            if (ch <= ' ') {
              continue;
            } else {
              throw new ParseException("Unexpected character '" + ch + "'", position);
            }
        }
      }

      return data;
    }
  }
}