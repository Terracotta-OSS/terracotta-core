package com.tc.license.util;

import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;

import com.tc.license.Capabilities;
import com.tc.license.Capability;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LicenseDescriptor {
  private static final String      DESCRIPTOR_RESOURCE = "/com/tc/license/license-descriptor.yml";
  private final Map                descriptor;
  private final Map                descriptionMap;
  private static LicenseDescriptor INSTANCE;

  public static synchronized LicenseDescriptor getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new LicenseDescriptor();
    }
    return INSTANCE;
  }

  private LicenseDescriptor() {
    InputStream in = getClass().getResourceAsStream(DESCRIPTOR_RESOURCE);
    if (in == null) throw new RuntimeException("Descriptor resource not found: " + DESCRIPTOR_RESOURCE);
    YamlReader reader = new YamlReader(new InputStreamReader(in));
    try {
      descriptor = (Map) reader.read();
    } catch (YamlException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        // ignored
      }
    }
    this.descriptionMap = (Map) get(descriptor, LicenseConstants.LICENSE_DESCRIPTION);
  }

  public Capabilities getCapabilities(String product) {
    Map productCapabilities = (Map) get(descriptor, LicenseConstants.PRODUCT_CAPABILITIES);
    List capabilitiesList = (List) get(productCapabilities, product);
    EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
    for (Iterator it = capabilitiesList.iterator(); it.hasNext();) {
      String capability = (String) it.next();
      capabilities.add(Capability.parse(capability));
    }
    return new Capabilities(capabilities);
  }

  public Capabilities getOpenSourceCapabilities() {
    return getCapabilities(LicenseConstants.ES);
  }

  public Map getDescriptionMap() {
    return descriptionMap;
  }

  public LicenseField createField(String fieldName) {
    Map fieldMap = (Map) get(descriptionMap, fieldName);
    String type = (String) get(fieldMap, LicenseConstants.TYPE);
    String pattern = (String) fieldMap.get(LicenseConstants.PATTERN);
    boolean required = Boolean.valueOf((String) get(fieldMap, LicenseConstants.REQUIRED));
    Map range = (Map) fieldMap.get(LicenseConstants.RANGE);
    return new TerracottaLicenseField(fieldName, type, pattern, required, range);
  }

  public List<LicenseField> getFields() {
    List<LicenseField> list = new ArrayList<LicenseField>();
    Set fieldNames = descriptionMap.keySet();
    for (Iterator it = fieldNames.iterator(); it.hasNext();) {
      String fieldName = (String) it.next();
      LicenseField field = createField(fieldName);
      list.add(field);
    }
    return list;
  }

  private static Object get(Map map, String key) {
    Object value = map.get(key);
    if (value != null) {
      return value;
    } else {
      throw new RuntimeException("Field " + key + " couldn't be found in resource: " + DESCRIPTOR_RESOURCE);
    }
  }
}
