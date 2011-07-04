/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.text;

import com.tc.util.NonPortableDetail;

/**
 * An interface for classes that know how to format {@link com.tc.util.NonPortableReason}.  Calling
 * {@link com.tc.util.NonPortableReason#accept(NonPortableReasonFormatter)} will cause the reason to call 
 * back to this interface with first the reason type, then all of the details, and finally the instructions. 
 */
public interface NonPortableReasonFormatter {
  /**
   * Format the reason type code
   * @param reasonType The type
   */
  public void formatReasonTypeName(byte reasonType);

  /**
   * Format the reason text
   * @param reasonText The reason message
   */
  public void formatReasonText(String reasonText);
  
  /**
   * Format a detail item
   * @param detail Detail in label/value form
   */
  public void formatDetail(NonPortableDetail detail);
  
  /**
   * Format the instructions on how to fix
   * @param instructionsText Instruction message
   */
  public void formatInstructionsText(String instructionsText);
  
  /**
   * Flush results if necessary
   */
  public void flush();
}
