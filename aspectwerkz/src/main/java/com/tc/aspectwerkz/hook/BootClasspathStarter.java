/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.hook;

/**
 * Starts a target process adding a dir in -Xbootclasspath/p: option <p/>Target process is launched using
 * <i>$JAVA_HOME/bin/java [opt] [main] </i> <br/>and [opt] is patched to use [bootDir] in -Xbootclasspath/p: option.
 * <br/>This is suitable for java 1.3. <br/>This can be use with java 1.4 to avoid running in JDWP mode.
 *
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class BootClasspathStarter extends AbstractStarter {
  private String bootDir;

  public BootClasspathStarter(String opt, String main, String bootDir) {
    super(opt, main);
    this.bootDir = bootDir;
    patchBootclasspath();
  }

  /**
   * add dir in first position of -Xbootclasspath/p option for target VM
   */
  private void patchBootclasspath() {
    // prepend dir in -Xbootclasspath/p:
    if (opt.indexOf("-Xbootclasspath/p:") < 0) {
      opt = "-Xbootclasspath/p:\"" + bootDir + "\" " + opt;

      //todo ? is \" ok on *nix
    } else {
      int index = -1;
      if (opt.indexOf("-Xbootclasspath/p:\"") >= 0) {
        // -Xbootclasspath/p: is defined using "
        index = opt.indexOf("-Xbootclasspath/p:\"") + "-Xbootclasspath/p:\"".length();
      } else if (opt.indexOf("-Xbootclasspath/p:'") >= 0) {
        // -Xbootclasspath/p: is defined using '
        index = opt.indexOf("-Xbootclasspath/p:'") + "-Xbootclasspath/p:'".length();
      } else {
        // -Xbootclasspath/p: is defined without quotes
        index = opt.indexOf("-Xbootclasspath/p:") + "-Xbootclasspath/p:".length();
      }
      StringBuffer optB = new StringBuffer("");
      optB.append(opt.substring(0, index));
      optB.append(bootDir);
      optB.append((System.getProperty("os.name", "").toLowerCase().indexOf("windows") >= 0) ? ";" : ":");
      optB.append(opt.substring(index));
      opt = optB.toString();
    }
  }
}
