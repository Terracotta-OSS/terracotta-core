/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bundles.exception;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.ResolverUtils;

import java.io.File;

public class MissingDefaultRepositoryException extends BundleException implements BundleExceptionSummary {

  private File repository;

  public MissingDefaultRepositoryException(final String msg) {
    super(msg);
  }

  public MissingDefaultRepositoryException(final String msg, final File repository) {
    super(msg);
    this.repository = repository;
  }

  public String getSummary() {
    StringBuffer buf = new StringBuffer(getMessage()).append("\n\n").append(INDENT);
    buf.append("You should set the value of the com.tc.l1.modules.repositories "
               + "system property to declare a valid default TIM repository.");
    if (repository != null) {
      buf.append("\n").append(INDENT);
      buf.append("Or, make sure that the following path resolves to a directory:\n\n").append(INDENT + INDENT);
      buf.append(ResolverUtils.canonicalize(repository)).append("\n\n").append(INDENT);
    }
    return StringUtils.replace(buf.toString(), "\n", System.getProperty("line.separator"));
  }

}
