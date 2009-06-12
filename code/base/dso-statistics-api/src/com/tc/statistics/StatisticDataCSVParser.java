/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.statistics;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

public class StatisticDataCSVParser {
  private final String line;
  private int          position = 0;

  public StatisticDataCSVParser(final String line) {
    this.line = line;
  }

  private char getNextChar() {
    if (position == line.length()) { return 0; }
    return line.charAt(position++);
  }

  private void setField(final StatisticData data, final int position, final String value) {
    if (null == value) { return; }
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
    fieldloop: while (true) {
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
            valueloop: while (true) {
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
