/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import java.util.Collection;

public class InstallOptionsHelper {

  private final Collection<InstallOption> options;

  public InstallOptionsHelper(Collection<InstallOption> options) {
    this.options = options;
  }

  public boolean force() {
    return isOptionSet(InstallOption.FORCE);
  }

  public boolean overwrite() {
    return isOptionSet(InstallOption.OVERWRITE) || force();
  }

  public boolean skipVerify() {
    return isOptionSet(InstallOption.SKIP_VERIFY);
  }

  public boolean verify() {
    return !skipVerify();
  }

  public boolean pretend() {
    return isOptionSet(InstallOption.PRETEND);
  }

  public boolean isOptionSet(InstallOption option) {
    return this.options.contains(option);
  }

}
