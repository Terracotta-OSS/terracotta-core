/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.definition;

//import org.dom4j.Attribute;
//import org.dom4j.Document;
//import org.dom4j.DocumentException;
//import org.dom4j.Element;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.tc.aspectwerkz.DeploymentModel;
import com.tc.aspectwerkz.expression.ExpressionInfo;
import com.tc.aspectwerkz.expression.ExpressionNamespace;
import com.tc.aspectwerkz.annotation.AspectAnnotationParser;
import com.tc.aspectwerkz.annotation.MixinAnnotationParser;
import com.tc.aspectwerkz.aspect.AdviceType;
import com.tc.aspectwerkz.exception.DefinitionException;
import com.tc.aspectwerkz.intercept.AdvisableImpl;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaMethodInfo;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.aspectwerkz.transform.inlining.AspectModelManager;
import com.tc.aspectwerkz.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Parses the XML definition using <tt>dom4j</tt>.
 *
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 * @author <a href="mailto:alex@gnilux.com">Alexandre Vasseur </a>
 */
public class DocumentParser {

  /**
   * Parses aspect class names.
   *
   * @param document the defintion as a document
   * @return the aspect class names
   */
  public static List parseAspectClassNames(Document document) {
    List aspectClassNames = new ArrayList();
    
    Element documentElement = document.getDocumentElement();
    NodeList systemNodes = documentElement.getChildNodes();
    for (int i = 0; i<systemNodes.getLength(); i++) {
      Node systemNode = systemNodes.item(i); 
      String nodeName = systemNode.getNodeName();
      if(nodeName.equals("system")) {
        String basePackage = getBasePackage((Element) systemNode);
        
        NodeList childNodes = systemNode.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
          Node childNode = childNodes.item(j);
          if(childNode.getNodeName().equals("aspect")) {
            addAspectClassName(aspectClassNames, childNode, basePackage);
            
          } else if(childNode.getNodeName().equals("package")) {
            NodeList aspectNodes = childNode.getChildNodes();
            for (int k = 0; k < aspectNodes.getLength(); k++) {
              Node aspectNode = aspectNodes.item(k);
              if(aspectNode.getNodeName().equals("aspect")) {
                addAspectClassName(aspectClassNames, aspectNode, basePackage);
              }
            }
          }
        }
      }
    }
    
    aspectClassNames.add(Virtual.class.getName());
    return aspectClassNames;
  }

  private static void addAspectClassName(List aspectClassNames, Node aspectNode, String basePackage) {
    if(aspectNode.getNodeName().equals("aspect")) {
      NamedNodeMap attrs = aspectNode.getAttributes();
      for (int l = 0; l < attrs.getLength(); l++) {
        Node attr = attrs.item(l);
        if(attr.getNodeName().equals("class")) {
          aspectClassNames.add(basePackage + ((Attr) attr).getValue().trim());
        }
      }
    }
  }

  /**
   * Parses the definition DOM document.
   *
   * @param xmlDef      the defintion xml
   * @param systemDef   the system definition
   * @param aspectClass the aspect class
   * @return the definition
   * @throws DocumentParserException 
   */
  public static AspectDefinition parseAspectDefinition(String xmlDef,
                                                       SystemDefinition systemDef,
                                                       Class aspectClass) {
    Document document;
    try {
      document = XmlParser.createDocument(xmlDef);
    } catch (IOException e) {
      throw new DefinitionException("Unable to parse definition; "+e.toString());
    }

    Element aspectElement = document.getDocumentElement();

    if (!"aspect".equals(aspectElement.getNodeName())) {
      throw new DefinitionException("XML definition for aspect is not well-formed: " + xmlDef);
    }
    String specialAspectName = null;
    String className = null;
    String deploymentModelAsString = null;
    String containerClassName = null;
    NamedNodeMap aspectAttributes = aspectElement.getAttributes();
    for (int i = 0; i < aspectAttributes.getLength(); i++) {
      Node attribute = aspectAttributes.item(i);
      String name = attribute.getNodeName().trim();
      if (name.equalsIgnoreCase("class")) {
        className = ((Attr) attribute).getValue().trim();
      } else if (name.equalsIgnoreCase("deployment-model")) {
        deploymentModelAsString = ((Attr) attribute).getValue().trim();
      } else if (name.equalsIgnoreCase("name")) {
        specialAspectName = ((Attr) attribute).getValue().trim();
      } else if (name.equalsIgnoreCase("container")) {
        containerClassName = ((Attr) attribute).getValue().trim();
      }
    }
    if (specialAspectName == null || specialAspectName.trim().length() == 0) {
      specialAspectName = className;
    }

    ClassInfo classInfo = JavaClassInfo.getClassInfo(aspectClass);
    ClassLoader loader = aspectClass.getClassLoader();

    // create the aspect definition
    AspectDefinition aspectDef = new AspectDefinition(specialAspectName, classInfo, systemDef);
    //TODO: if this XML centric deployment is supposed to PRESERVE @Aspect values, then it is broken
    aspectDef.setContainerClassName(containerClassName);
    aspectDef.setDeploymentModel(DeploymentModel.getDeploymentModelFor(deploymentModelAsString));

    parsePointcutElements(aspectElement, aspectDef); //needed to support undefined named pointcut in Attributes AW-152

    // load the different aspect model and let them define their aspects
    AspectModelManager.defineAspect(classInfo, aspectDef, loader);

    // parse the aspect info
    parseParameterElements(aspectElement, aspectDef);
    parsePointcutElements(aspectElement, aspectDef); //reparse pc for XML override (AW-152)
    parseAdviceElements(aspectElement, aspectDef, JavaClassInfo.getClassInfo(aspectClass));
    parseIntroduceElements(aspectElement, aspectDef, "", aspectClass.getClassLoader());
    return aspectDef;
  }

  /**
   * Parses the definition DOM document.
   *
   * @param loader   the current class loader
   * @param document the defintion as a document
   * @return the definitions
   */
  public static Set parse(ClassLoader loader, Document document) {
    // parse the transformation scopes
    return parseSystemElements(loader, document.getDocumentElement());
  }

  /**
   * Parses the <tt>system</tt> elements.
   *
   * @param loader the current class loader
   * @param root   the root element
   */
  private static Set parseSystemElements(ClassLoader loader, Element root) {
    Set systemDefs = new HashSet();
    
    NodeList rootNodes = root.getChildNodes();
    for (int i = 0; i < rootNodes.getLength(); i++) {
      Node childNode = rootNodes.item(i);
      if(childNode.getNodeName().equals("system")) {
        Element systemElement = (Element) childNode;
        SystemDefinition definition = parseSystemElement(loader, systemElement, getBasePackage(systemElement));
        if (definition != null) {
          systemDefs.add(definition);
        }
      }
    }
    return systemDefs;
  }

  /**
   * Parses the <tt>system</tt> elements.
   *
   * @param loader        the current class loader
   * @param systemElement the system element
   * @param basePackage   the base package
   * @return the definition for the system
   */
  private static SystemDefinition parseSystemElement(ClassLoader loader,
                                                     Element systemElement,
                                                     String basePackage) {
    String uuid = systemElement.getAttribute("id");
    if (uuid == null || uuid.equals("")) {
      throw new DefinitionException("system UUID must be specified");
    }
    SystemDefinition definition = new SystemDefinition(uuid);

    // add the virtual aspect
    addVirtualAspect(definition);

    // parse the global pointcuts
    List globalPointcuts = parseGlobalPointcutDefs(systemElement);
    
    //FIXME: systemDef should link a namespace, + remove static hashmap in Namespace (uuid clash in parallel CL)
    ExpressionNamespace systemNamespace = ExpressionNamespace.getNamespace(definition.getUuid());
    for (Iterator iterator = globalPointcuts.iterator(); iterator.hasNext();) {
      PointcutInfo pointcutInfo = (PointcutInfo) iterator.next();
      systemNamespace.addExpressionInfo(
              pointcutInfo.name, new ExpressionInfo(pointcutInfo.expression, systemNamespace.getName())
      );
    }

    // parse the global deployment scopes definitions
    parseDeploymentScopeDefs(systemElement, definition);

    // parse the global advisable definitions
    parseAdvisableDefs(systemElement, definition);

    // parse the include, exclude and prepare elements
    parseIncludePackageElements(systemElement, definition, basePackage);
    parseExcludePackageElements(systemElement, definition, basePackage);
    parsePrepareElements(systemElement, definition, basePackage);

    // parse without package elements
    parseAspectElements(loader, systemElement, definition, basePackage, globalPointcuts);

    // parse without package elements
    parseMixinElements(loader, systemElement, definition, basePackage);

    // parse with package elements
    parsePackageElements(loader, systemElement, definition, basePackage, globalPointcuts);

    // add all deployment scopes to the virtual advice
    DefinitionParserHelper.attachDeploymentScopeDefsToVirtualAdvice(definition);

    return definition;
  }

  /**
   * Parses the global pointcuts.
   *
   * @param systemElement the system element
   * @return a list with the pointcuts
   */
  private static List parseGlobalPointcutDefs(Element systemElement) {
    List globalPointcuts = new ArrayList();
    
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("pointcut")) {
        Element pointcutElement = (Element) childNode;
        PointcutInfo pointcutInfo = new PointcutInfo(pointcutElement.getAttribute("name"), //
                                                     pointcutElement.getAttribute("expression").trim());
        // pointcut CDATA is expression unless already specified as an attribute
        if (pointcutInfo.expression == null || pointcutInfo.expression.trim().length() == 0) {
          pointcutInfo.expression = getText(pointcutElement).trim().replace('\n', ' ');
        }
        globalPointcuts.add(pointcutInfo);
      }
    }

    return globalPointcuts;
  }

  /**
   * Parses the global deployment-scope elements.
   *
   * @param systemElement the system element
   * @param definition
   */
  private static void parseDeploymentScopeDefs(Element systemElement, SystemDefinition definition) {
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("deployment-scope")) {
        Element deploymentScopeElement = (Element) childNode;
        String name = deploymentScopeElement.getAttribute("name");
        String expression = deploymentScopeElement.getAttribute("expression");
        // pointcut CDATA is expression unless already specified as an attribute
        if (expression == null || expression.trim().length() == 0) {
          expression = getText(deploymentScopeElement).trim();
        }
        DefinitionParserHelper.createAndAddDeploymentScopeDef(name, expression, definition);
      }
    }
  }

  /**
   * Parses the global advisable elements.
   * 
   * @param systemElement the system element
   * @param definition
   */
  private static void parseAdvisableDefs(Element systemElement, SystemDefinition definition) {
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("advisable")) {
        Element advisableElement = (Element) childNode;
        String pointcutTypes = advisableElement.getAttribute("pointcut-type");
        if (pointcutTypes == null || pointcutTypes.trim().length() == 0) {
          pointcutTypes = "all";
        }
        String expression = advisableElement.getAttribute("expression");
        // pointcut CDATA is expression unless already specified as an attribute
        if (expression == null || expression.trim().length() == 0) {
          expression = getText(advisableElement).trim();
        }
        handleAdvisableDefinition(definition, expression, pointcutTypes);
      }
    }
  }

  /**
   * Parses the definition DOM document.
   * 
   * @param loader the current class loader
   * @param systemElement the system element
   * @param definition the definition
   * @param basePackage the base package
   * @param globalPointcuts the global pointcuts
   */
  private static void parsePackageElements(ClassLoader loader, Element systemElement, SystemDefinition definition,
                                           String basePackage, List globalPointcuts) {
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("package")) {
        Element packageElement = (Element) childNode;
        String packageName = basePackage + getPackage(packageElement);
        parseAspectElements(loader, packageElement, definition, packageName, globalPointcuts);
        parseMixinElements(loader, packageElement, definition, packageName);
        parseAdvisableDefs(packageElement, definition);
      }
    }
  }

  /**
   * Parses the <tt>aspect</tt> elements.
   * 
   * @param loader the current class loader
   * @param systemElement the system element
   * @param definition the definition object
   * @param packageName the package name
   * @param globalPointcuts the global pointcuts
   */
  private static void parseAspectElements(ClassLoader loader,
                                          Element systemElement,
                                          SystemDefinition definition,
                                          String packageName,
                                          List globalPointcuts) {
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("aspect")) {
        Element aspectElement = (Element) childNode;
        
        String aspectName = aspectElement.getAttribute("name");
        String className = aspectElement.getAttribute("class");
        String deploymentModel = aspectElement.getAttribute("deployment-model");
        String containerClassName = aspectElement.getAttribute("container");

        // class is mandatory
        if (Strings.isNullOrEmpty(className)) {
          System.err.println("Warning: could not load aspect without 'class=..' attribute");
          new Exception().printStackTrace();
          continue;
        }

        String aspectClassName = packageName + className;
        if (aspectName == null || aspectName.trim().length() == 0) {
          aspectName = aspectClassName;
        }

        // create the aspect definition
        ClassInfo aspectClassInfo;
        try {
          aspectClassInfo = AsmClassInfo.getClassInfo(aspectClassName, loader);
        } catch (Exception e) {
          System.err.println("Warning: could not load aspect " + aspectClassName + " from " + loader + "; "
                             + e.toString());
          e.printStackTrace();
          continue;
        }

        AspectDefinition aspectDef = new AspectDefinition(aspectName, aspectClassInfo, definition);

        // add the global pointcuts to the aspect
        for (Iterator it = globalPointcuts.iterator(); it.hasNext();) {
          PointcutInfo pointcutInfo = (PointcutInfo) it.next();
          DefinitionParserHelper.createAndAddPointcutDefToAspectDef(
                  pointcutInfo.name,
                  pointcutInfo.expression,
                  aspectDef
          );
        }
        parsePointcutElements(aspectElement, aspectDef); //needed to support undefined named pointcut in Attributes AW-152

        // load the different aspect model and let them define their aspects
        AspectModelManager.defineAspect(aspectClassInfo, aspectDef, loader);

        // parse the class bytecode annotations
        AspectAnnotationParser.parse(aspectClassInfo, aspectDef, loader);

        // XML definition settings always overrides attribute definition settings AW-357
        if (!Strings.isNullOrEmpty(deploymentModel)) {
          aspectDef.setDeploymentModel(DeploymentModel.getDeploymentModelFor(deploymentModel));
        }
        if (!Strings.isNullOrEmpty(aspectName)) {
          aspectDef.setName(aspectName);
        }
        if (!Strings.isNullOrEmpty(containerClassName)) {
          aspectDef.setContainerClassName(containerClassName);
        }

        // parse the aspect info
        parseParameterElements(aspectElement, aspectDef);
        parsePointcutElements(aspectElement, aspectDef); // reparse pc for XML override (AW-152)
        parseAdviceElements(aspectElement, aspectDef, aspectClassInfo);
        parseIntroduceElements(aspectElement, aspectDef, packageName, loader);

        definition.addAspect(aspectDef);
      }
    }
  }

  /**
   * Parses the <tt>mixin</tt> elements.
   *
   * @param loader           the current class loader
   * @param systemElement    the system element
   * @param systemDefinition the system definition
   * @param packageName      the package name
   */
  private static void parseMixinElements(ClassLoader loader,
                                         Element systemElement,
                                         SystemDefinition systemDefinition,
                                         String packageName) {
    NodeList childNodes = systemElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("mixin")) {
        Element mixinElement = (Element) childNode;
        
        String className = mixinElement.getAttribute("class");
        String deploymentModelAsString = mixinElement.getAttribute("deployment-model");
        boolean isTransient = false;
        boolean isTransientSetInXML = false;
        String transientValue = mixinElement.getAttribute("transient"); 
        if(transientValue!=null) {
          isTransient = transientValue.equalsIgnoreCase("true");
          isTransientSetInXML = true;
        }
        
        String factoryClassName = mixinElement.getAttribute("factory");
        String expression = mixinElement.getAttribute("bind-to");
        
        String mixinClassName = packageName + className;

        // create the mixin definition
        ClassInfo mixinClassInfo;
        try {
          mixinClassInfo = AsmClassInfo.getClassInfo(mixinClassName, loader);
        } catch (Exception e) {
          System.err.println("Warning: could not load mixin " + mixinClassName + " from " + loader + "; "
                             + e.toString());
          e.printStackTrace();
          continue;
        }

        DeploymentModel deploymentModel = deploymentModelAsString == null
                                          || deploymentModelAsString.trim().length() == 0 ? DeploymentModel.PER_INSTANCE
            : DeploymentModel.getDeploymentModelFor(deploymentModelAsString);

        MixinDefinition mixinDefinition = DefinitionParserHelper.createAndAddMixinDefToSystemDef(mixinClassInfo,
                                                                                                 expression,
                                                                                                 deploymentModel,
                                                                                                 isTransient,
                                                                                                 systemDefinition);

        // parse the class bytecode annotations
        MixinAnnotationParser.parse(mixinClassInfo, mixinDefinition);

        // XML definition settings always overrides attribute definition settings if present
        if (!Strings.isNullOrEmpty(deploymentModelAsString)) {
          mixinDefinition.setDeploymentModel(DeploymentModel.getDeploymentModelFor(deploymentModelAsString));
        }
        if (!Strings.isNullOrEmpty(factoryClassName)) {
          mixinDefinition.setFactoryClassName(factoryClassName);
        }
        if (isTransientSetInXML) {
          mixinDefinition.setTransient(isTransient);
        }

        parseParameterElements(mixinElement, mixinDefinition);
      }
    }
  }

  /**
   * Adds a virtual system aspect to the definition. Needed to do various tricks.
   *
   * @param definition
   */
  public static void addVirtualAspect(SystemDefinition definition) {
    Class clazz = Virtual.class;
    String aspectName = clazz.getName();
    ClassInfo aspectClassInfo = JavaClassInfo.getClassInfo(clazz);
    AspectDefinition aspectDef = new AspectDefinition(aspectName, aspectClassInfo, definition);
    try {
      MethodInfo methodInfo = JavaMethodInfo.getMethodInfo(clazz.getDeclaredMethod("virtual", new Class[]{}));
      aspectDef.addBeforeAdviceDefinition(
              new AdviceDefinition(
                      methodInfo.getName(),
                      AdviceType.BEFORE,
                      null,
                      aspectName,
                      aspectName,
                      null,
                      methodInfo,
                      aspectDef
              )
      );
    } catch (NoSuchMethodException e) {
      throw new Error("virtual aspect [" + aspectName + "] does not have expected method: " + e.toString());
    }
    definition.addAspect(aspectDef);
  }

  /**
   * Parses the aspectElement parameters.
   *
   * @param aspectElement the aspect element
   * @param aspectDef     the aspect def
   */
  private static void parseParameterElements(Element aspectElement, AspectDefinition aspectDef) {
    NodeList childNodes = aspectElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("param")) {
        Element paramElement = (Element) childNode;
        aspectDef.addParameter(paramElement.getAttribute("name"), paramElement.getAttribute("value"));
      }
    }
  }

  /**
   * Parses the mixinElement parameters.
   *
   * @param mixinElement the mixin element
   * @param mixinDef     the mixin def
   */
  private static void parseParameterElements(Element mixinElement, MixinDefinition mixinDef) {
    NodeList childNodes = mixinElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("param")) {
        Element paramElement = (Element) childNode;
        mixinDef.addParameter(paramElement.getAttribute("name"), paramElement.getAttribute("value"));
      }
    }
  }

  /**
   * Parses the pointcuts.
   * 
   * @param aspectElement the aspect element
   * @param aspectDef the system definition
   */
  private static void parsePointcutElements(Element aspectElement, AspectDefinition aspectDef) {
    NodeList aspectNodes = aspectElement.getChildNodes();
    for (int i = 0; i < aspectNodes.getLength(); i++) {
      Node childNode = aspectNodes.item(i);
      if(childNode.getNodeType()!=Node.ELEMENT_NODE) {
        continue;
      }
      Element childElement = (Element) childNode;
      if (childElement.getNodeName().equals("pointcut")) {
        String name = childElement.getAttribute("name");
        String expression = childElement.getAttribute("expression");
        // pointcut CDATA is expression unless already specified as an attribute
        if (expression == null || expression.trim().length() == 0) {
          expression = getText(childElement).trim();
        }
        DefinitionParserHelper.createAndAddPointcutDefToAspectDef(name, expression, aspectDef);
      } else if (childElement.getNodeName().equals("deployment-scope")) {
        String name = childElement.getAttribute("name");
        String expression = childElement.getAttribute("expression");
        // pointcut CDATA is expression unless already specified as an attribute
        if (expression == null) {
          expression = getText(childElement).trim();
        }
        DefinitionParserHelper.createAndAddDeploymentScopeDef(
                name, expression, aspectDef.getSystemDefinition()
        );
      } else if (childElement.getNodeName().equals("advisable")) {
        String expression = childElement.getAttribute("expression");
        String pointcutTypes = childElement.getAttribute("pointcut-type");
        if (expression == null || expression.trim().length() == 0) {
          expression = getText(childElement).trim();
        }
        handleAdvisableDefinition(aspectDef.getSystemDefinition(), expression, pointcutTypes);
      }
    }
  }
  
  public static String getText(Element element) {
    NodeList childNodes = element.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if(childNode.getNodeType()==Node.TEXT_NODE) {
        return ((Text) childNode).getData();
      }
    }
    return "";
  }

  /**
   * Parses the advices.
   *
   * @param aspectElement   the aspect element
   * @param aspectDef       the system definition
   * @param aspectClassInfo the aspect class
   */
  private static void parseAdviceElements(Element aspectElement, AspectDefinition aspectDef, ClassInfo aspectClassInfo) {
    NodeList childNodes = aspectElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("advice")) {
        Element adviceElement = (Element) childNode;

        String name = adviceElement.getAttribute("name");
        String type = adviceElement.getAttribute("type");
        String bindTo = adviceElement.getAttribute("bind-to");

        MethodInfo method = DefinitionParserHelper.createMethodInfoForAdviceFQN(name, aspectDef, aspectClassInfo);
        DefinitionParserHelper.createAndAddAdviceDefsToAspectDef(type, bindTo, name, method, aspectDef);

        NodeList bindNodes = adviceElement.getChildNodes();
        for (int j = 0; j < bindNodes.getLength(); j++) {
          Node bindToNode = bindNodes.item(j);
          if (bindToNode.getNodeName().equals("bind-to")) {
            Element bindToElement = (Element) bindToNode;
            String pointcut = bindToElement.getAttribute("pointcut");
            DefinitionParserHelper.createAndAddAdviceDefsToAspectDef(type, pointcut, name, method, aspectDef);
          }
        }
      }
    }
  }

  /**
   * Parses the interface introductions.
   * 
   * @param aspectElement the aspect element
   * @param aspectDef the system definition
   * @param packageName
   * @param loader
   */
  private static void parseIntroduceElements(Element aspectElement,
                                             AspectDefinition aspectDef,
                                             String packageName,
                                             ClassLoader loader) {
    NodeList childNodes = aspectElement.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("introduce")) {
        Element introduceElement = (Element) childNode;
        
        String klass = introduceElement.getAttribute("class");
        String name = introduceElement.getAttribute("name");
        String bindTo = introduceElement.getAttribute("bind-to");

        // default name = FQN
        String fullClassName = packageName + klass;
        if (name == null || name.length() == 0) {
          name = fullClassName;
        }

        // load the class info to determine if it is a pure interface introduction
        ClassInfo introductionClassInfo;
        try {
          introductionClassInfo = AsmClassInfo.getClassInfo(fullClassName, loader);
        } catch (Exception e) {
          throw new DefinitionException("could not find interface introduction: " + packageName + klass + "; "
                                        + e.getMessage());
        }

        // pure interface introduction
        if (introductionClassInfo.isInterface()) {
          DefinitionParserHelper
              .createAndAddInterfaceIntroductionDefToAspectDef(bindTo, name, fullClassName, aspectDef);

          // handles nested "bind-to" elements
          NodeList bindToNodes = introduceElement.getChildNodes();
          for (int j = 0; j < bindToNodes.getLength(); j++) {
            Node bindToNode = bindToNodes.item(j);
            if (bindToNode.getNodeName().equals("bindTo")) {
              Element bindToElement = (Element) bindToNode;
              String pointcut = bindToElement.getAttribute("pointcut");
              DefinitionParserHelper.createAndAddInterfaceIntroductionDefToAspectDef(pointcut, name, fullClassName,
                                                                                     aspectDef);
            }
          }
        }
      }
    }
  }

  /**
   * Retrieves and returns the package.
   *
   * @param packageElement the package element
   * @return the package as a string ending with DOT, or empty string
   */
  private static String getPackage(Element packageElement) {
    String packageName = packageElement.getAttribute("name");
    if (packageName != null) {
      if (packageName.endsWith(".*")) {
        return packageName.substring(0, packageName.length() - 1);
      } else if (!packageName.endsWith(".")) { 
        return packageName + "."; 
      }
    }
    return "";
  }

  /**
   * Parses the <tt>include</tt> elements.
   *
   * @param root        the root element
   * @param definition  the definition object
   * @param packageName the package name
   */
  private static void parseIncludePackageElements(Element root,
                                                  SystemDefinition definition,
                                                  String packageName) {
    NodeList childNodes = root.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node includeNode = childNodes.item(i);
      if (includeNode.getNodeName().equals("include")) {
        Element includeElement = (Element) includeNode;
        String packageValue = includeElement.getAttribute("package");

        String includePackage = "";
        // handle base package
        if (packageName.endsWith(".*")) {
          includePackage = packageName.substring(0, packageName.length() - 2);
        } else if (packageName.endsWith(".")) {
          includePackage = packageName.substring(0, packageName.length() - 1);
        }

        // handle exclude package
        includePackage = packageName + packageValue.trim();
        if (includePackage.endsWith(".*")) {
          includePackage = includePackage.substring(0, includePackage.length() - 2);
        } else if (includePackage.endsWith(".")) {
          includePackage = includePackage.substring(0, includePackage.length() - 1);
        }
        if (includePackage.length() != 0) {
          definition.addIncludePackage(includePackage);
        }
      }
    }
  }

  /**
   * Parses the <tt>exclude</tt> elements.
   *
   * @param root        the root element
   * @param definition  the definition object
   * @param packageName the package name
   */
  private static void parseExcludePackageElements(Element root, SystemDefinition definition, String packageName) {
    NodeList childNodes = root.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("exclude")) {
        Element excludeElement = (Element) childNode;

        String excludeValue = excludeElement.getAttribute("package");

        String excludePackage = "";
        // handle base package
        if (packageName.endsWith(".*")) {
          excludePackage = packageName.substring(0, packageName.length() - 2);
        } else if (packageName.endsWith(".")) {
          excludePackage = packageName.substring(0, packageName.length() - 1);
        }

        // handle exclude package
        excludePackage = packageName + excludeValue.trim();
        if (excludePackage.endsWith(".*")) {
          excludePackage = excludePackage.substring(0, excludePackage.length() - 2);
        } else if (excludePackage.endsWith(".")) {
          excludePackage = excludePackage.substring(0, excludePackage.length() - 1);
        }
        if (excludePackage.length() != 0) {
          definition.addExcludePackage(excludePackage);
        }
      }
    }
  }

  /**
   * Parses the <tt>prepare</tt> elements.
   * 
   * @param root the root element
   * @param definition the definition object
   * @param packageName the base package name
   */
  private static void parsePrepareElements(Element root,
                                          SystemDefinition definition,
                                          String packageName) {
    NodeList childNodes = root.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals("prepare")) {
        Element prepareElement = (Element) childNode;
        String packageValue = prepareElement.getAttribute("package");
        
        String preparePackage = "";

        // handle base package
        if (packageName.endsWith(".*")) {
          preparePackage = packageName.substring(0, packageName.length() - 2);
        } else if (packageName.endsWith(".")) {
          preparePackage = packageName.substring(0, packageName.length() - 1);
        }

        // handle prepare package
        preparePackage = packageName + packageValue.trim();
        if (preparePackage.endsWith(".*")) {
          preparePackage = preparePackage.substring(0, preparePackage.length() - 2);
        } else if (preparePackage.endsWith(".")) {
          preparePackage = preparePackage.substring(0, preparePackage.length() - 1);
        }
        if (preparePackage.length() != 0) {
          definition.addPreparePackage(preparePackage);
        }
      }
    }
  }

  /**
   * Retrieves and returns the base package for a system element
   *
   * @param system a system element
   * @return the base package
   */
  private static String getBasePackage(Element system) {
    String basePackage = "";
    NamedNodeMap attrs = system.getAttributes();
    for (int i = 0; i < attrs.getLength(); i++) {
      Node item = attrs.item(i);
      if (item.getNodeName().equalsIgnoreCase("base-package")) {
        basePackage = ((Attr) item).getValue().trim();
        if (basePackage.endsWith(".*")) {
          basePackage = basePackage.substring(0, basePackage.length() - 1);
        } else if (!basePackage.endsWith(".")) {
          basePackage += ".";
        }
        break;
      }
    }
    
/*    
    for (Iterator it2 = system.attributeIterator(); it2.hasNext();) {
      Attribute attribute = (Attribute) it2.next();
      if (attribute.getName().trim().equalsIgnoreCase("base-package")) {
        basePackage = attribute.getValue().trim();
        if (basePackage.endsWith(".*")) {
          basePackage = basePackage.substring(0, basePackage.length() - 1);
        } else if (basePackage.endsWith(".")) {
          // skip
        } else {
          basePackage += ".";
        }
        break;
      } else {
        continue;
      }
    }
*/    
    return basePackage;
  }

  /**
   * Struct with pointcut info.
   */
  private static class PointcutInfo {
    public String name;
    public String expression;
    
    public PointcutInfo(String name, String expression) {
      this.name = name;
      this.expression = expression;
    }
  }

  /**
   * Handles the advisable definition.
   *
   * @param definition
   * @param withinPointcut
   * @param pointcutTypes
   */
  private static void handleAdvisableDefinition(SystemDefinition definition,
                                                String withinPointcut,
                                                String pointcutTypes) {
    // add the Advisable Mixin with the expression defined to the system definitions
    definition.addMixinDefinition(
            DefinitionParserHelper.createAndAddMixinDefToSystemDef(
                    AdvisableImpl.CLASS_INFO,
                    withinPointcut,
                    DeploymentModel.PER_INSTANCE,
                    false, // advisble mixin is NOT transient
                    definition
            )
    );

    boolean hasAllPointcuts = false;
    boolean hasExecutionPointcut = false;
    boolean hasCallPointcut = false;
    boolean hasSetPointcut = false;
    boolean hasGetPointcut = false;
    boolean hasHandlerPointcut = false;
    if (pointcutTypes == null ||
            pointcutTypes.equals("") ||
            pointcutTypes.equalsIgnoreCase("all")) {
      hasAllPointcuts = true;
    } else {
      StringTokenizer tokenizer = new StringTokenizer(pointcutTypes, "|");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        if (token.trim().equalsIgnoreCase("all")) {
          hasAllPointcuts = true;
          break;
        } else if (token.trim().equalsIgnoreCase("execution")) {
          hasExecutionPointcut = true;
        } else if (token.trim().equalsIgnoreCase("call")) {
          hasCallPointcut = true;
        } else if (token.trim().equalsIgnoreCase("set")) {
          hasSetPointcut = true;
        } else if (token.trim().equalsIgnoreCase("getDefault")) {
          hasGetPointcut = true;
        } else if (token.trim().equalsIgnoreCase("handler")) {
          hasHandlerPointcut = true;
        }
      }
    }
    if (hasAllPointcuts || hasExecutionPointcut) {
      DefinitionParserHelper.createAndAddAdvisableDef(
              // TODO add ctor to expression - BUT: problem with mixin and ctor, ordering issue, Jp.invoke() calls field instance that has not been init yet in ctor (since body not invoked)
              //"(( execution(!static * *.*(..)) || execution(*.new(..)) ) && " + withinPointcut + ')',
              // we exclude static method execution since we need the advisable instance
              "(execution(!static * *.*(..)) && " + withinPointcut + ')',
              definition
      );
    }
    if (hasAllPointcuts || hasCallPointcut) {
      DefinitionParserHelper.createAndAddAdvisableDef(
              // TODO add ctor to expression - BUT: problem with mixin and ctor, ordering issue, Jp.invoke() calls field instance that has not been init yet in ctor (since body not invoked)                    //"(call(!static * " + typePattern + ".*(..)) || call(" + typePattern + ".new(..)))",
              // we exclude static method withincode since we need the advisable instance
              // as a consequence, withincode(staticinitialization(..)) is also excluded
              "(call(* *.*(..)) && withincode(!static * *.*(..)) && " + withinPointcut + ')',
              definition
      );
    }
    if (hasAllPointcuts || hasSetPointcut) {
      DefinitionParserHelper.createAndAddAdvisableDef(
              // we exclude static method withincode since we need the advisable instance
              // as a consequence, withincode(staticinitialization(..)) is also excluded
              "(set(* *.*) && withincode(!static * *.*(..)) && " + withinPointcut + ')',
              definition
      );
    }
    if (hasAllPointcuts || hasGetPointcut) {
      DefinitionParserHelper.createAndAddAdvisableDef(
              // we exclude static method withincode since we need the advisable instance
              // as a consequence, withincode(staticinitialization(..)) is also excluded
              "(getDefault(* *.*) && withincode(!static * *.*(..))  && " + withinPointcut + ')',
              definition
      );
    }
    if (hasAllPointcuts || hasHandlerPointcut) {
      DefinitionParserHelper.createAndAddAdvisableDef(
              // we exclude static method withincode since we need the advisable instance
              // as a consequence, withincode(staticinitialization(..)) is also excluded
              "(handler(*..*) && withincode(!static * *.*(..))  && " + withinPointcut + ')',
              definition
      );
    }
  }
}
