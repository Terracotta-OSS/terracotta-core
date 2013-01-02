/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.util;

import java.io.IOException;

public interface ClusteredStringBuilder extends Appendable, CharSequence {
  @Override
  ClusteredStringBuilder append(CharSequence csq) throws IOException;

  @Override
  ClusteredStringBuilder append(CharSequence csq, int start, int end) throws IOException;

  @Override
  ClusteredStringBuilder append(char c) throws IOException;
}
