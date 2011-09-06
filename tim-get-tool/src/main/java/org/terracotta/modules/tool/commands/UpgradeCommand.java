/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.xmlbeans.XmlException;
import org.terracotta.modules.tool.InstallListener;
import org.terracotta.modules.tool.InstallOption;
import org.terracotta.modules.tool.Module;

import com.tc.config.Loader;
import com.tc.text.Banner;
import com.tc.util.StringUtil;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UpgradeCommand extends ModuleOperatorCommand {

  private static final String             LONGOPT_OVERWRITE = "overwrite";
  private static final String             LONGOPT_FORCE     = "force";
  private static final String             LONGOPT_DRYRUN    = "dry-run";
  private static final String             LONGOPT_NOVERIFY  = "no-verify";

  private final Collection<InstallOption> installOptions;

  public UpgradeCommand() {
    options.addOption(buildOption(LONGOPT_OVERWRITE, "Install anyway, even if already installed"));
    options.addOption(buildOption(LONGOPT_FORCE, "Synonym to overwrite"));
    options.addOption(buildOption(LONGOPT_DRYRUN, "Do not perform actual installation"));
    options.addOption(buildOption(LONGOPT_NOVERIFY, "Skip checksum verification"));
    arguments.put("file", "The path to tc-config.xml");
    installOptions = new ArrayList<InstallOption>();
  }

  @Override
  public String name() {
    return "upgrade";
  }

  @Override
  public String syntax() {
    return "<file> {options}";
  }

  @Override
  public String description() {
    return "Upgrade your tc-config.xml to latest versions of TIMs and " + StringUtil.LINE_SEPARATOR
           + "                  install them if needed";
  }

  public void execute(CommandLine cli) {
    if (cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.FORCE);
    if (cli.hasOption(LONGOPT_OVERWRITE) || cli.hasOption(LONGOPT_FORCE)) installOptions.add(InstallOption.OVERWRITE);
    if (cli.hasOption(LONGOPT_DRYRUN)) installOptions.add(InstallOption.DRYRUN);
    if (cli.hasOption(LONGOPT_NOVERIFY)) installOptions.add(InstallOption.SKIP_VERIFY);
    try {
      process(cli);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (XmlException e) {
      throw new RuntimeException(e);
    }
    printEpilogue();
  }

  private void process(CommandLine cli) throws IOException, XmlException {
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

    if (cli.hasOption(LONGOPT_DRYRUN)) {
      Banner.infoBanner("--dry-run detected: no modules will actually be installed during this execution.");
    }

    InstallListener listener = new DefaultInstallListener(report, out);

    boolean updateConfig = false;
    TcConfigDocument tcConfigDocument = new Loader().parse(tcConfigPath);
    TcConfig tcConfig = tcConfigDocument.getTcConfig();
    if (tcConfig.getClients() == null || tcConfig.getClients().getModules() == null) {
      out.print("Found no module to upgrade.");
      return;
    }

    com.terracottatech.config.Module[] xmlModules = tcConfig.getClients().getModules().getModuleArray();

    for (com.terracottatech.config.Module xmlModule : xmlModules) {
      String version = xmlModule.getVersion();
      String versionStr = (version == null) ? "latest" : version;

      out.print("* Parsing module: " + xmlModule.getName() + ":" + versionStr);
      Module latest = modules.findLatest(xmlModule.getName(), xmlModule.getGroupId());
      boolean neededToInstall = false;

      if (latest == null) {
        out.println(": No module found on server");
      } else {
        if (!latest.isInstalled()) {
          neededToInstall = true;
        } else {
          if (version != null && latest.version().compareTo(version) > 0) {
            // if version is null, then tc-config did not specify a version and no need to update tc-config
            // if versions is specified, but older than latest, also need to update
            neededToInstall = true;
          } else {
            out.println(": up to date");
          }
        }
      }

      if (neededToInstall) {
        out.println(": latest version " + latest.version());
        if (!cli.hasOption(LONGOPT_DRYRUN)) {
          latest.install(listener, actionLog(), installOptions);
        }
        out.println();

        // Don't update config if module version is not specified - it will automatically pick up the latest
        if (version != null) {
          actionLog.addModifiedModuleAction(xmlModule.getGroupId(), xmlModule.getName(), xmlModule.getVersion(), latest
              .version());

          xmlModule.setVersion(latest.version());
          updateConfig = true;
        }
      }
    }

    if (!cli.hasOption(LONGOPT_DRYRUN)) {
      // save original file to .original if found newer module
      if (updateConfig && !cli.hasOption(LONGOPT_DRYRUN)) {
        File originalFile = new File(tcConfigPath.getAbsolutePath() + ".original");
        FileUtils.copyFile(tcConfigPath, originalFile);
        out.println("Your original config file has been saved to " + originalFile);
        tcConfigDocument.save(tcConfigPath);
      } else {
        out.println();
        out.println("Found no module that requires upgrade.");
      }
    }
  }

  private void printEpilogue() {
    out.println("\nDone.");
  }
}
