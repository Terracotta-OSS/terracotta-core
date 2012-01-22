/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.text.NonPortableReasonFormatter;

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class NonPortableReasonTest extends TestCase {
  private NonPortableReason reason;

  public void setUp() {
    this.reason = new NonPortableReason(getClass(), NonPortableReason.TEST_REASON);
  }

  public void testAcceptFormatter() {
    TestNonPortableReasonFormatter formatter = new TestNonPortableReasonFormatter();
    this.reason.addDetail("label1", "value1");
    this.reason.addDetail("label2", "value2");
    this.reason.accept(formatter);

    // make sure the detail text was formatted
    assertEquals(1, formatter.formatReasonTextCalls.size());

    // make sure the details were formatted
    assertEquals(2, formatter.formatDetailsCalls.size());
    assertReasonEquals("label1", "value1", formatter.formatDetailsCalls.get(0));
    assertReasonEquals("label2", "value2", formatter.formatDetailsCalls.get(1));

    // make sure the instruction text was formatted
    assertEquals(1, formatter.formatInstructionsTextCalls.size());
  }

  private void assertReasonEquals(String label, String value, Object o) {
    NonPortableDetail detail = (NonPortableDetail)o;
    assertEquals(label, detail.getLabel());
    assertEquals(value, detail.getValue());
  }

  private static final class TestNonPortableReasonFormatter implements NonPortableReasonFormatter {

    public final List formatReasonTextCalls = new LinkedList();
    public final List formatDetailsCalls = new LinkedList();
    public final List formatInstructionsTextCalls = new LinkedList();
    public final List flushCalls = new LinkedList();

    public void formatReasonText(String reasonText) {
      formatReasonTextCalls.add(reasonText);
    }

    public void formatDetail(NonPortableDetail detail) {
      formatDetailsCalls.add(detail);
    }

    public void formatInstructionsText(String instructionsText) {
      formatInstructionsTextCalls.add(instructionsText);
    }

    public void flush() {
      flushCalls.add(new Object());
    }

    public void formatReasonTypeName(byte reasonType) {
      return;
    }
  }
}
