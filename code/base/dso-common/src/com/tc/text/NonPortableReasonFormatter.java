/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.text;

import com.tc.util.NonPortableDetail;

public interface NonPortableReasonFormatter {
  public void formatReasonTypeName(byte reasonType);
  public void formatReasonText(String reasonText);
  public void formatDetail(NonPortableDetail detail);
  public void flush();
}
