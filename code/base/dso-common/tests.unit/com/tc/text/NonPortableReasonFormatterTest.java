/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.text;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.util.NonPortableDetail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class NonPortableReasonFormatterTest extends TestCase {

  private NonPortableReasonFormatter formatter;
  private PrintWriter                out;
  private TestParagraphFormatter     paragraphFormatter;
  private StringFormatter            stringFormatter;
  private StringWriter               stringWriter;
  private String                     separator;

  @Override
  public void setUp() {
    stringFormatter = new StringFormatter();
    stringWriter = new StringWriter();
    out = new PrintWriter(stringWriter);
    separator = ":  ";
    paragraphFormatter = new TestParagraphFormatter();
    formatter = new ConsoleNonPortableReasonFormatter(out, separator, stringFormatter, paragraphFormatter);
  }

  public void testBasics() {

    String reasonText = "my reason text";
    formatter.formatReasonText(reasonText);
    assertEquals(1, paragraphFormatter.formatCalls.size());
    assertEquals(reasonText, paragraphFormatter.formatCalls.get(0));

    // check the formatting of details
    NonPortableDetail detail1 = new NonPortableDetail("0123456789", "0123456789");
    NonPortableDetail detail2 = new NonPortableDetail("0123", "0123");
    formatter.formatDetail(detail1);
    formatter.formatDetail(detail2);

    StringBuffer expected = new StringBuffer();
    expected.append(reasonText + stringFormatter.newline());
    expected.append(stringFormatter.newline());
    
    expected.append("For more information on this issue, please visit our Troubleshooting Guide at:" + 
                    stringFormatter.newline());                    
    expected.append(TCNonPortableObjectError.NPOE_TROUBLE_SHOOTING_GUIDE + stringFormatter.newline());
    expected.append(stringFormatter.newline());
    
    expected.append("0123456789" + separator + "0123456789" + stringFormatter.newline());
    expected.append("0123      " + separator + "0123");

    // check formatting
    formatter.flush();
    System.err.println("Expecting: <" + expected + ">, found: <" + stringWriter.getBuffer().toString() + ">");
    assertEquals(expected.toString(), stringWriter.getBuffer().toString());
    formatter.flush();
    assertEquals(expected.toString(), stringWriter.getBuffer().toString());
  }

  private static final class TestParagraphFormatter implements ParagraphFormatter {

    public final List formatCalls = new LinkedList();

    public String format(String in) {
      formatCalls.add(in);
      return in;
    }
  }
}
