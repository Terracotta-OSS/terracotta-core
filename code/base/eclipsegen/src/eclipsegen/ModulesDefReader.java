package eclipsegen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ModulesDefReader {

  @SuppressWarnings("unchecked")
  public static Module[] readFrom(Map data) {
    List modules = new ArrayList();

    List moduleList = (List) data.get("modules");
    if (moduleList == null) { throw new RuntimeException("modules list is null"); }

    for (Iterator i = moduleList.iterator(); i.hasNext();) {
      Map module = (Map) i.next();

      if (module.size() != 1) { throw new RuntimeException("modules map is wrong size (" + module.size() + "): "
                                                           + module); }

      Map.Entry entry = (Entry) module.entrySet().iterator().next();
      String moduleName = (String) entry.getKey();
      ModuleBuilder builder = new ModuleBuilder(moduleName);

      Map moduleAttrs = (Map) entry.getValue();

      for (Iterator attrIter = moduleAttrs.entrySet().iterator(); attrIter.hasNext();) {
        Map.Entry attr = (Entry) attrIter.next();

        String section = (String) attr.getKey();
        Object vals = attr.getValue();

        if ("options".equals(section)) {
          for (Iterator valsIterator = ((Map) vals).entrySet().iterator(); valsIterator.hasNext();) {
            Entry val = (Entry) valsIterator.next();
            builder.addOption((String) val.getKey(), val.getValue().toString());
          }
        } else if ("dependencies".equals(section)) {
          for (Iterator depsIterator = ((List) vals).iterator(); depsIterator.hasNext();) {
            builder.addDependency((String) depsIterator.next());
          }

        } else {
          throw new AssertionError("unknown section: " + section);
        }
      }

      modules.add(builder.build());
    }

    return (Module[]) modules.toArray(new Module[modules.size()]);
  }
}
