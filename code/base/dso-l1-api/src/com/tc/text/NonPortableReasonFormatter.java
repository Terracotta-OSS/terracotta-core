/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.text;

import com.tc.util.NonPortableDetail;

public interface NonPortableReasonFormatter {
  public void formatReasonTypeName(byte reasonType);
  public void formatReasonText(String reasonText);
  public void formatDetail(NonPortableDetail detail);
  public void formatInstructionsText(String instructionsText);
  public void flush();
}
