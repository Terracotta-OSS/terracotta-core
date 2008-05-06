/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package eclipsegen;

import java.util.HashMap;
import java.util.Map;

public class Jdk {
  private static final Map<String, Jdk> tcbuildVals = new HashMap<String, Jdk>();

  public static final Jdk               JDK_14      = new Jdk("1.4", "J2SE-1.4", "J2SE-1.4");
  public static final Jdk               JDK_15      = new Jdk("1.5", "J2SE-1.5", "J2SE-1.5");
  public static final Jdk               JDK_16      = new Jdk("1.6", "JavaSE-1.6", "JavaSE-1.6");

  private final String                  raw;
  private final String                  eclipse;

  private Jdk(String raw, String eclipse, String tcbuild) {
    this.raw = raw;
    this.eclipse = eclipse;

    Jdk prev = tcbuildVals.put(tcbuild, this);
    if (prev != null) { throw new RuntimeException("duplicating mapping for " + tcbuild); }
  }

  public String getEclipse() {
    return eclipse;
  }

  public String getRaw() {
    return raw;
  }

  public static Jdk getForTcbuild(String val) {
    return tcbuildVals.get(val);
  }

}
