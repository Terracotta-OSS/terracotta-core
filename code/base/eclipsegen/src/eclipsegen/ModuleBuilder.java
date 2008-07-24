package eclipsegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleBuilder {

  final String              moduleName;
  final Map<String, String> options = new HashMap<String, String>();
  final Set<String>         deps    = new HashSet<String>();

  public ModuleBuilder(String moduleName) {
    this.moduleName = moduleName;
  }

  public void addOption(String name, String value) {
    options.put(name, value);
  }

  public void addDependency(String depName) {
    boolean added = deps.add(depName);
    if (!added) {
      System.err.println("WARN: Duplicate dependency [" + depName + "] in module " + moduleName);
    }
  }

  public Module build() {
    return new Module(this);
  }

}
