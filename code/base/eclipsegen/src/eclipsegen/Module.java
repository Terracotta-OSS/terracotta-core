package eclipsegen;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Module {

  private final String      name;
  private final Jdk         jdk;
  private final boolean     isModule;
  private final Set<String> deps;
  private final boolean     isAspectJ;
  private final boolean     javaodc;

  Module(ModuleBuilder builder) {
    this.name = builder.moduleName;
    this.jdk = validateJdk(builder.options);
    this.isModule = getBooleanOption(builder.options, "module", false);
    this.deps = Collections.unmodifiableSet(new TreeSet<String>(builder.deps));
    this.isAspectJ = getBooleanOption(builder.options, "aspectj", false);
    this.javaodc = getBooleanOption(builder.options, "javadoc", false);

    if (!builder.options.isEmpty()) { throw new RuntimeException("options not read: " + builder.options); }
  }

  private boolean getBooleanOption(Map<String, String> options, String key, boolean def) {
    String val = options.remove(key);
    if (val == null) { return def; }

    return Boolean.valueOf(val.toLowerCase()).booleanValue();
  }

  private Jdk validateJdk(Map<String, String> options) {
    String val = options.remove("jdk");
    if (val == null) { throw new RuntimeException("missing jdk for " + name); }

    Jdk rv = Jdk.getForTcbuild(val);
    if (rv == null) { throw new RuntimeException("invalid JDK string: " + val); }
    return rv;
  }

  public String getName() {
    return name;
  }

  public boolean isModule() {
    return isModule;
  }

  public Jdk getJdk() {
    return jdk;
  }

  public Set<String> getDependencies() {
    return deps;
  }

  public boolean isAspectJ() {
    return isAspectJ;
  }

  public boolean isJavaodc() {
    return javaodc;
  }

}
