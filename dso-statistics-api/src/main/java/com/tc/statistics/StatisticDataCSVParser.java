/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.statistics;

import com.tc.util.Assert;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

public class StatisticDataCSVParser {

  public final static String CURRENT_CSV_VERSION = "1.0";
  public final static String CURRENT_CSV_HEADER  = "Session ID,IP,Differentiator,Moment,Name,Element,Data Number,Data Text,Data Date,Data Decimal\n";

  private final String       line;
  private int                position            = 0;

  /**
   * Creates a new data instance from a single line of CSV data. This parser assumes that there are exactly as many CSV
   * fields as there are properties in {@code StatisticData}. The expected order is: sessionId, agentIp,
   * agentDifferentiator, moment, name, element, numeric data, text data, date data, and decimal data. None of the field
   * are allowed to contain new lines, and all of the fields should be delimiter by double quotes. Refer to
   * {@link #toCsv} for the rules about escaped characters.
   * 
   * @param dataFormatVersion the version identifier that corresponds to the provided CSV text line
   * @param line the line of text that contains the fields for a single {@code StatisticData} instance
   * @return the {@code StatisticData} instance that corresponds to the provided CSV line
   * @throws ParseException when the provided format version is not supported; or when the provided CSV text couldn't be
   *         parsed successfully
   */
  public static StatisticData newInstanceFromCsvLine(final String dataFormatVersion, final String line)
      throws ParseException {
    Assert.assertNotNull("dataFormatVersion", dataFormatVersion);

    if (CURRENT_CSV_VERSION.equals(dataFormatVersion)) {
      return new StatisticDataCSVParser(line).parse();
    } else {
      throw new ParseException("The data format version '" + dataFormatVersion + "' is not supported.", 0);
    }
  }

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
        data.setData(Long.valueOf(Long.parseLong(value)));
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
