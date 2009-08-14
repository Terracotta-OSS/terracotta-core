/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.xmlbeans.XmlException;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.InstallOption;
import org.terracotta.modules.tool.Module;

import com.tc.config.Loader;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InstallForCommand extends ModuleOperatorCommand {

  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_DRYRUN   = "dry-run";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Collection<InstallOption> installOptions;

  public InstallForCommand() {
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Install anyway, even if already installed"));
    options.addOption(buildOption(LONGOPT_FORCE, "Synonym to overwrite"));
    options.addOption(buildOption(LONGOPT_DRYRUN, "Do not perform actual installation"));
    options.addOption(buildOption(LONGOPT_NOVERIFY, "Skip checksum verification"));
    arguments.put("file", "The path to tc-config.xml");
    installOptions = new ArrayList<InstallOption>();
  }

  @Override
  public String name() {
    return "install-for";
  }

  @Override
  public String syntax() {
    return "<file> {options}";
  }

  @Override
  public String description() {
    return "Parse and install TIMs declared in your tc-config.xml";
  }

  public void execute(CommandLine cli) {
    if (cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.FORCE);
    if (cli.hasOption(LONGOPT_OVERWRITE) || cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.OVERWRITE);
    if (cli.hasOption(LONGOPT_DRYRUN)) installOptions.add(InstallOption.DRYRUN);
    if (cli.hasOption(LONGOPT_NOVERIFY)) installOptions.add(InstallOption.SKIP_VERIFY);
    process(cli);
    printEpilogue();
  }

  private void process(CommandLine cli) {
    List<String> args = cli.getArgList();
    if (args.isEmpty()) {
      out.println("You need to specify the path to your tc-config.xml");
      return;
    }

    File tcConfigPath = new File(args.get(0));
    if (!tcConfigPath.exists() || !tcConfigPath.isFile()) {
      out.println("File '" + tcConfigPath + "' doesn't exist or not a file.");
      return;
    }

    List<Module> neededToInstalledModules;
    try {
      neededToInstalledModules = parseModules(tcConfigPath);
    } catch (IOException e) {
      throw new RuntimeException("Error reading from config file" + tcConfigPath, e);
    } catch (XmlException e) {
      throw new RuntimeException("Error parsing from config file" + tcConfigPath, e);
    }

    if (neededToInstalledModules.isEmpty()) {
      out.println("Found no module to install.");
      return;
    }

    InstallListener listener = new DefaultInstallListener(report, out);
    for (Module module : neededToInstalledModules) {
      module.install(listener, actionLog(), installOptions);
    }

  }

  private List<Module> parseModules(File tcConfigPath) throws IOException, XmlException {
    List<Module> list = new ArrayList<Module>();
    TcConfigDocument tcConfigDocument = new Loader().parse(tcConfigPath);
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    if (tcConfig.getClients() == null || tcConfig.getClients().getModules() == null) { return Collections.EMPTY_LIST; }
    com.terracottatech.config.Module[] xmlModules = tcConfig.getClients().getModules().getModuleArray();
    for (com.terracottatech.config.Module xmlModule : xmlModules) {
      List<Module> found = modules.find(Arrays.asList(new String[] { xmlModule.getName(), xmlModule.getVersion(),
          xmlModule.getGroupId() }));
      
      String versionStr = (xmlModule.getVersion() == null) ? "latest" : xmlModule.getVersion();
      out.println("Parsing module: " + xmlModule.getName() + ":" + versionStr);
      out.flush();
      if (found.isEmpty()) {
        err.println("No module found matching: " + xmlModule.getName() + ":" + versionStr + " groupId="
                    + xmlModule.getGroupId());
        err.flush();
      }
      list.addAll(found);
    }
    return list;
  }

  private void printEpilogue() {
    out.println("\nDone.");
  }
}
