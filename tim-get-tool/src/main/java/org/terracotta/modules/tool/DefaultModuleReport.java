/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.tc.util.runtime.Os;
import com.tc.util.version.VersionMatcher;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class DefaultModuleReport extends ModuleReport {

  private static final String INSTALLED_MARKER    = "+";
  private static final String NOTINSTALLED_MARKER = "-";
  private static final String OUTOFDATE_MARKER    = "!";

  private static final int    INDENT_WIDTH        = 3;
  private static final String INDENT_CHAR         = " ";
  private static final String DEFAULT_GROUPID     = "org.terracotta.modules";
  private static final String LEGEND              = "(+) Installed  (-) Not installed  (!) Installed but newer version exists";

  private String canonicalize(File path) {
    try {
      return path.getCanonicalFile().toString();
    } catch (IOException e) {
      return path.toString();
    }
  }

  private String marker(Module module) {
    if (!module.isInstalled()) return NOTINSTALLED_MARKER;
    return module.isLatest() ? INSTALLED_MARKER : OUTOFDATE_MARKER;
  }

  private boolean isUsingDefaultGroupId(AbstractModule module) {
    return DEFAULT_GROUPID.equals(module.groupId());
  }

  private String indent(String text) {
    StringBuffer result = new StringBuffer();
    String[] lines = StringUtils.split(text, '\n');
    for (String line : lines) {
      result.append(StringUtils.repeat(INDENT_CHAR, INDENT_WIDTH)).append(line).append('\n');
    }
    return StringUtils.chomp(result.toString());
  }

  @Override
  public String title(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.print(module.artifactId().concat(" ").concat(module.version()).concat(""));
    if (!isUsingDefaultGroupId(module)) out.print(" [" + module.groupId() + "]");

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String headline(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.print("(" + marker((Module) module) + ") ");
    out.print(title(module));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String digest(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println(headline(module));
    out.println(" ");

    out.println("Installed: " + (module.isInstalled() ? "YES" : "NO"));
    out.println(" ");
    if (!StringUtils.isEmpty(module.vendor())) out.println("Author   : " + module.vendor());
    if (!StringUtils.isEmpty(module.copyright())) out.println("Copyright: " + module.copyright());
    if (!StringUtils.isEmpty(module.website().toString())) out.println("Homepage : " + module.website());
    if (!StringUtils.isEmpty(module.contactAddress())) out.println("Contact  : " + module.contactAddress());
    if (!StringUtils.isEmpty(module.docUrl().toString())) out.println("Docs     : " + module.docUrl());
    out.println("Download : " + module.repoUrl());
    out.println("Status   : " + module.tcProjectStatus());
    out.println("Internal : " + module.tcInternalTIM() + "\n");

    if (!StringUtils.isEmpty(module.description())) {
      out.println(module.description().replaceAll("\n[ ]+", "\n"));
      out.println();
    }

    String compatibility = "any Terracotta version.";
    if (!module.tcVersion().equals(VersionMatcher.ANY_VERSION)) {
      compatibility = "Terracotta " + module.tcVersion();
    } else if (!module.timApiVersion().equals(VersionMatcher.ANY_VERSION)) {
      compatibility = "Terracotta TIM API " + module.timApiVersion();
    }
    out.println("Compatible with " + compatibility);

    String text = indent(writer.toString());
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String dependencies(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.println("Dependencies:\n ");
    if (module.dependencies().isEmpty()) out.println(indent("None."));

    for (AbstractModule dependency : module.dependencies()) {
      String line = "o " + dependency.artifactId() + " " + dependency.version();
      if (!isUsingDefaultGroupId(dependency)) line = line.concat(" [" + dependency.groupId() + "]");
      out.println(indent(line));
    }

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String mavenCoordinates(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println("Maven Coordinates:\n ");
    out.println(indent("groupId   : " + module.groupId()));
    out.println(indent("artifactId: " + module.artifactId()));
    out.println(indent("version   : " + module.version()));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  private String configInfo(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    Document document;

    try {
      document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }

    Element parent = document.createElement("modules");
    Element child = document.createElement("module");
    parent.appendChild(child);
    child.setAttribute("name", module.artifactId());
    child.setAttribute("version", module.version());
    if (!isUsingDefaultGroupId(module)) child.setAttribute("group-id", module.groupId());
    document.appendChild(parent);

    out.println("Configuration:\n ");
    try {
      serialize(document, out);
    } catch (Exception e) {
      out.println(e.getMessage());
    }

    out.println("Note: If you are installing the newest or only version, the version may be omitted.");

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  public void serialize(Document doc, Writer out) throws Exception {
    TransformerFactory tfactory = TransformerFactory.newInstance();
    Transformer serializer;
    try {
      serializer = tfactory.newTransformer();
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      serializer.transform(new DOMSource(doc), new StreamResult(out));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
  }

  private String installationInfo(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    if (module.isInstalled()) {
      Modules owner = module.owner();

      // XXX compute actual location if installed - remember that
      // a TIM may also be installed at the root of the repository
      File location = module.installLocationInRepository(owner.repository());
      File actualLocation = location.exists() ? location.getParentFile() : owner.repository();
      out.println("Installed at " + canonicalize(actualLocation));

      if (module.isLatest()) out.println("This is the latest version.\n ");
      else out.println("A newer version is available.\n ");
    }

    if (module.versions().isEmpty()) {
      out.println("There are no other versions of this TIM that are compatible with the current installation.");
    } else {
      out.println("The following versions are also available:\n ");
      List<Module> siblings = module.siblings();
      Collections.reverse(siblings);
      for (Module sibling : siblings) {
        String marker = "(" + (sibling.isInstalled() ? INSTALLED_MARKER : NOTINSTALLED_MARKER) + ") ";
        String line = marker + sibling.version();
        out.println(indent(line));
      }
      out.println("\n ");
    }

    // XXX NEED A BETTER WAY TO PRESENT THIS INSTALL/UPDATE INSTRUCTION
    if (!module.isInstalled() || !module.isLatest()) {
      out.println("Issue the following command to install the latest version:\n ");
      String script = "tim-get.";
      script += (Os.isWindows() ? "bat" : "sh");
      script += INDENT_CHAR + (module.isInstalled() ? "update" : "install");
      script += INDENT_CHAR + module.artifactId();
      script += INDENT_CHAR + module.version();
      if (isUsingDefaultGroupId(module)) script += INDENT_CHAR + module.groupId();
      out.println(indent(script));
    }

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String summary(Module module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);

    out.println(digest(module));
    out.println();
    out.println(indent(dependencies(module)));
    out.println();
    out.println(indent(mavenCoordinates(module)));
    out.println();

    if (module.installsAsModule()) {
      out.println(indent(configInfo(module)));
      out.println();
    }

    out.println(indent(installationInfo(module)));

    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String footer(AbstractModule module) {
    StringWriter writer = new StringWriter();
    PrintWriter out = new PrintWriter(writer);
    out.println(LEGEND);
    String text = writer.toString();
    return StringUtils.chomp(StringUtils.trim(text));
  }

  @Override
  public String header(AbstractModule module) {
    return StringUtils.EMPTY;
  }
}
