package com.tc.license.util;

import net.sourceforge.yamlbeans.YamlException;
import net.sourceforge.yamlbeans.YamlReader;

import com.tc.license.Capability;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LicenseDescriptor {
  private static final String DESCRIPTOR_RESOURCE = "license-descriptor.yml";
  private final Map           descriptor;
  private final Map           descriptionMap;

  public LicenseDescriptor() {
    this(DESCRIPTOR_RESOURCE);
  }

  public LicenseDescriptor(String descriptorResource) {
    InputStream in = LicenseDescriptor.class.getResourceAsStream(descriptorResource);
    if (in == null) throw new LicenseException("Descriptor resource not found: " + descriptorResource);
    YamlReader reader = new YamlReader(new InputStreamReader(in));
    try {
      descriptor = (Map) reader.read();
    } catch (YamlException e) {
      throw new LicenseException(e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        // ignored
      }
    }
    this.descriptionMap = (Map) get(descriptor, LicenseConstants.LICENSE_DESCRIPTION);
  }

  public Set<Capability> getLicensedCapabilities(String product, String edition) {
    if (LicenseConstants.PRODUCT_CUSTOM.equals(product)) {
      return getEnterpriseCapabilities();
    }
    Map licensedProducts = (Map) get(descriptor, LicenseConstants.LICENSED_PRODUCT);
    Map editions = (Map) get(licensedProducts, product);
    List capablities = (List) get(editions, edition);
    return convertToCapabilitySet(capablities);
  }

  public Set<Capability> getEnterpriseCapabilities() {
    List values = (List) get(descriptor, LicenseConstants.ENTERPRISE_CAPABILITIES);
    return convertToCapabilitySet(values);
  }

  public Set<Capability> getOpenSourceCapabilities() {
    List values = (List) get(descriptor, LicenseConstants.OPENSOURCE_CAPABILITIES);
    return convertToCapabilitySet(values);
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
      throw new LicenseException("Field " + key + " couldn't be found in resource: " + DESCRIPTOR_RESOURCE);
    }
  }

  private Set<Capability> convertToCapabilitySet(List values) {
    Set<Capability> result = new HashSet<Capability>();
    for (Iterator it = values.iterator(); it.hasNext();) {
      Capability c = new Capability((String) it.next());
      result.add(c);
    }
    return result;
  }
}
