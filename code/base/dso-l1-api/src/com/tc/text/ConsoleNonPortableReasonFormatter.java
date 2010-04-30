/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.text;

import com.tc.exception.TCNonPortableObjectError;
import com.tc.util.NonPortableDetail;
import com.tc.util.NonPortableReason;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Format a {@link com.tc.util.NonPortableReason} to a PrintWriter, such as System.out or System.err.
 */
public class ConsoleNonPortableReasonFormatter implements NonPortableReasonFormatter {

  private final PrintWriter        out;
  private final StringFormatter    stringFormatter;
  private final Collection         details;
  private String                   instructions;
  private int                      maxLabelLength;
  private final String             separator;
  private final ParagraphFormatter paragraphFormatter;

  /**
   * @param out Writer to format to
   * @param separator Separator character to use in details between label and value
   * @param stringFormatter Can format strings with padding and such
   * @param paragraphFormatter Can format paragraphs nicely (word wrap, etc)
   */
  public ConsoleNonPortableReasonFormatter(PrintWriter out, String separator, StringFormatter stringFormatter,
                                           ParagraphFormatter paragraphFormatter) {
    this.out = out;
    this.separator = separator;
    this.stringFormatter = stringFormatter;
    this.paragraphFormatter = paragraphFormatter;
    details = new LinkedList();
  }

  public void formatReasonTypeName(byte type) {
    String name;
    switch(type) {
      case NonPortableReason.CLASS_NOT_ADAPTABLE:
        name = "CLASS_NOT_ADAPTABLE";
        break;
      case NonPortableReason.SUPER_CLASS_NOT_ADAPTABLE:
        name = "SUPER_CLASS_NOT_ADAPTABLE";
        break;
      case NonPortableReason.SUBCLASS_OF_LOGICALLY_MANAGED_CLASS:
        name = "SUBCLASS_OF_LOGICALLY_MANAGED_CLASS";
        break;
      case NonPortableReason.CLASS_NOT_IN_BOOT_JAR:
        name = "CLASS_NOT_IN_BOOT_JAR";
        break;
      case NonPortableReason.CLASS_NOT_INCLUDED_IN_CONFIG:
        name = "CLASS_NOT_INCLUDED_IN_CONFIG";
        break;
      case NonPortableReason.SUPER_CLASS_NOT_INSTRUMENTED:
        name = "SUPER_CLASS_NOT_INSTRUMENTED";
        break;
      default:
        name = "UNDEFINED";
    }
    out.print(name);
  }
  
  public void formatDetail(NonPortableDetail detail) {
    details.add(detail);
    if (detail.getLabel().length() > maxLabelLength) {
      maxLabelLength = detail.getLabel().length();
    }
  }

  public void flush() {
    for (Iterator i = details.iterator(); i.hasNext();) {
      NonPortableDetail detail = (NonPortableDetail) i.next();
      out.print(stringFormatter.rightPad(maxLabelLength, detail.getLabel()));
      out.print(separator);
      out.print(detail.getValue());
      if (i.hasNext()) {
        out.println();
      }
    }
    details.clear();
    if (instructions != null) {
      out.println();
      out.print(instructions);
    }
    out.flush();
  }

  public void formatReasonText(String reasonText) {
    if (null == reasonText || 0 == reasonText.length()) return;
      
    out.println(paragraphFormatter.format(reasonText));
    out.println();
    out.println("For more information on this issue, please visit our Troubleshooting Guide at:");
    out.println(TCNonPortableObjectError.NPOE_TROUBLE_SHOOTING_GUIDE);
    out.println();
  }

  public void formatInstructionsText(String instructionsText) {
    if (null == instructionsText || 0 == instructionsText.length()) {
      instructions = null;
    }
     
    instructions = instructionsText;
  }

}
