/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.defaults;

import org.apache.xmlbeans.SchemaLocalAttribute;
import org.apache.xmlbeans.SchemaParticle;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.util.Assert;

import java.util.regex.Pattern;

/**
 * A {@link DefaultValueProvider} that gets defaults from the schema itself.
 */
public class SchemaDefaultValueProvider implements DefaultValueProvider {

  public SchemaDefaultValueProvider() {
  // Nothing here.
  }

  // This makes sure we don't try to interpret XPaths that have anything other than normal element delimiters in them.
  private static final Pattern ALLOWED_COMPONENT_PATTERN = Pattern.compile("@?[A-Za-z0-9][A-Za-z0-9-]*");

  public boolean isOptional(SchemaType baseType, String xpath) throws XmlException {
    return fetchParticle(baseType, xpath).isOptional();
  }

  public boolean possibleForXPathToHaveDefault(String xpath) {
    return isInterpretableXPath(xpath);
  }

  public XmlObject defaultFor(SchemaType baseType, String xpath) throws XmlException {
    XmlObject out = fetchDefault(baseType, xpath);
    if (out == null) throw new XmlException("The element at XPath '" + xpath + "' has no default specified.");
    else return out;
  }

  public boolean hasDefault(SchemaType baseType, String xpath) throws XmlException {
    return fetchDefault(baseType, xpath) != null;
  }

  private XmlObject fetchDefault(SchemaType baseType, String xpath) throws XmlException {
    SchemaInfo info = fetchParticle(baseType, xpath);
    if (!info.isDefault()) return null;
    else return info.defaultValue();
  }

  private static class SchemaInfo {
    private final boolean   isDefault;
    private final boolean   isOptional;
    private final XmlObject defaultValue;

    public SchemaInfo(boolean isDefault, boolean isOptional, XmlObject defaultValue) {
      Assert.eval(defaultValue == null || isDefault);
      Assert.eval(defaultValue == null || isOptional);

      this.isDefault = isDefault;
      this.isOptional = isOptional;
      this.defaultValue = defaultValue;
    }

    public boolean isDefault() {
      return this.isDefault;
    }

    public boolean isOptional() {
      return this.isOptional;
    }

    public XmlObject defaultValue() {
      return this.defaultValue;
    }
  }

  // This is the single most incomprehensible piece of code in the entire system. I have absolutely no idea what this
  // XPath stuff is actually supposed to do; there is almost no documentation on it. I made this work by long
  // trial-and-error. You're on your own here, mate.
  private SchemaInfo fetchParticle(SchemaType baseType, String xpath) throws XmlException {
    Assert.assertNotNull(baseType);
    Assert.assertNotBlank(xpath);

    if (!isInterpretableXPath(xpath)) {
      // formatting
      throw new XmlException(
          "Right now, our default-finding code doesn't support anything other than a path consisting "
              + "of only simple elements. '" + xpath + "' is not such a path.");
    }

    String[] components = xpath.split("/");
    SchemaParticle currentParticle = baseType.getContentModel();
    Assert.assertNotNull(currentParticle);

    boolean anyAreOptional = false;

    if (components.length == 1 && components[0].startsWith("@")) {
      // We don't yet support direct attribute grabs for defaults; this is because I can't figure out how to get
      // XMLBeans to do them right.
      return new SchemaInfo(false, currentParticle.getMinOccurs().intValue() == 0, null);
    }

    for (int i = 0; i < components.length; ++i) {
      String component = components[i];
      if (currentParticle.getMinOccurs().intValue() == 0) anyAreOptional = true;

      int particleType = currentParticle.getParticleType();

      // Attributes should've been caught on the last time through.
      if (component.startsWith("@")) {
        // formatting
        throw new XmlException("Component '" + component + "' of XPath '" + xpath
            + "' specifies an attribute in an invalid position.");
      }

      if ((i == components.length - 2) && (components[i + 1].startsWith("@"))) {
        String attributeName = components[i + 1].substring(1);

        if (currentParticle.getType() == null || currentParticle.getType().getAttributeModel() == null) {
          // formatting
          throw new XmlException("The element purportedly containing attribute '" + attributeName + "' in XPath '"
              + xpath + "' seems to have no attributes at all.");
        }

        SchemaLocalAttribute[] attributes = currentParticle.getType().getAttributeModel().getAttributes();
        for (int j = 0; j < attributes.length; ++j) {
          if (attributes[j].getName().getLocalPart().equals(attributeName)) { return new SchemaInfo(attributes[j]
              .isDefault(), attributes[j].getMinOccurs().intValue() == 0, attributes[j].getDefaultValue()); }
        }

        throw new XmlException("Attribute '" + attributeName + "' of element '" + component + "' in XPath '" + xpath
            + "' was not found.");
      }

      if (particleType == SchemaParticle.ELEMENT) {
        if (currentParticle.getName().getLocalPart().equals(component)) {
          if (i == components.length - 1) break;
          else {
            currentParticle = currentParticle.getType().getContentModel();
            Assert.assertNotNull(currentParticle);
            continue;
          }
        } else {
          throw new XmlException("Component '" + component + "' of XPath '" + xpath
              + "' not found; we have one element only, '" + currentParticle.getName().getLocalPart() + "'.");
        }
      }

      checkParticleType(component, particleType, xpath);

      SchemaParticle[] children = currentParticle.getParticleChildren();
      if (children == null) {
        // formatting
        throw new XmlException("Component '" + component + "' of XPath '" + xpath + "' seems to have "
            + "no children. Stop.");
      }

      ElementReturn elementReturn = findNextElement(xpath, component, children, i == components.length - 1);
      SchemaParticle next = elementReturn.particle();
      anyAreOptional = anyAreOptional || elementReturn.isOptional();

      if (next == null) {
        // formatting
        throw new XmlException("Component '" + component + "' of XPath '" + xpath
            + "' was not found. Please check the path " + "and try again.");
      }

      currentParticle = next;
    }

    if (currentParticle.getMinOccurs().intValue() == 0) anyAreOptional = true;

    if (currentParticle.getParticleType() != SchemaParticle.ELEMENT) {
      // formatting
      throw new XmlException("XPath '" + xpath
          + "' points to a complex type or other item, not a single element. Stop.");
    }

    if (currentParticle.isDefault() && (!anyAreOptional)) {
      // formatting
      throw new XmlException("XPath '" + xpath + "' has a default, but is not optional. This doesn't make sense.");
    }

    return new SchemaInfo(currentParticle.isDefault(), anyAreOptional, currentParticle.getDefaultValue());
  }

  private static class ElementReturn {
    private final SchemaParticle particle;
    private final boolean        isOptional;

    public ElementReturn(SchemaParticle particle, boolean isOptional) {
      Assert.assertNotNull(particle);
      this.particle = particle;
      this.isOptional = isOptional;
    }

    public SchemaParticle particle() {
      return this.particle;
    }

    public boolean isOptional() {
      return this.isOptional;
    }
  }

  private ElementReturn findNextElement(String xpath, String component, SchemaParticle[] children, boolean lastOne)
      throws XmlException {

    SchemaParticle next = null;
    StringBuffer actualChildren = new StringBuffer();
    boolean optional = false;

    for (int childIndex = 0; childIndex < children.length; ++childIndex) {
      String thisChildName = children[childIndex].getName().getLocalPart();
      if (childIndex > 0) actualChildren.append(", ");
      actualChildren.append(thisChildName);
      if (thisChildName != null && thisChildName.equals(component)) {
        if (next != null) {
          // formatting
          throw new XmlException("Component '" + component + "' of XPath '" + xpath + "' has multiple children named '"
              + component + "'. We don't support this. Stop.");
        } else {
          next = children[childIndex];
        }
      }
    }

    if ((!lastOne) && next.getParticleType() == SchemaParticle.ELEMENT) {
      optional = optional || next.getMinOccurs().intValue() == 0;
      next = next.getType().getContentModel();
    }

    if (next == null) {
      // formatting
      throw new XmlException("Component '" + component + "' of path '" + xpath + "' was not found. Instead, we found: "
          + actualChildren);
    }

    optional = optional || next.getMinOccurs().intValue() == 0;

    return new ElementReturn(next, optional);
  }

  private void checkParticleType(String component, int particleType, String xpath) throws XmlException {
    if (particleType != SchemaParticle.ALL && particleType != SchemaParticle.CHOICE
        && particleType != SchemaParticle.SEQUENCE) {
      // formatting
      throw new XmlException("Component '" + component + "' of XPath '" + xpath + "' is a schema particle of type "
          + particleType + ", not " + SchemaParticle.ALL + " ('all'), " + SchemaParticle.CHOICE + " ('choice'), or "
          + SchemaParticle.SEQUENCE + " ('sequence'). Stop.");
    }
  }

  private boolean isInterpretableXPath(String xpath) {
    String[] components = xpath.split("/");

    for (int i = 0; i < components.length; ++i) {
      if (!ALLOWED_COMPONENT_PATTERN.matcher(components[i]).matches()) return false;
    }

    return true;
  }
}
