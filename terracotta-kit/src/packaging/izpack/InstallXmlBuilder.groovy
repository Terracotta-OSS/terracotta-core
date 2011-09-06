import groovy.xml.MarkupBuilder

/**
 * Utility class for interacting with the Maven POM.
 */
class Pom {
  static project
  static String property(String name) {
    project.properties[name]
  }

  static boolean isEnterprise() {
    property("kit.edition") == "enterprise"
  }
}
Pom.project = project

/**
 * Utility class for working with IzPack resources.
 */
class IzPack {
  static File kitDirectory() {
    new File(Pom.property("rootDir"))
  }

  /**
   * The IzPack installer breaks the installation process down into "packs",
   * which are nothing more than collections of files that are installed on
   * disk, and displays the name of each pack being installed as part of the
   * progress indicator in the installer. This method creates one pack for each
   * top-level directory of the kit, plus one additional pack that represents
   * the files in the top-level directory itself. This makes for a more
   * informative installation progress report.
   */
  static packs() {
    def result = []
    def topLevelFilesPack = [
      name: "misc",
      files: []
    ]
    result.add(topLevelFilesPack)

    kitDirectory().eachFile { file ->
      if (file.isDirectory()) {
        def pack = [name: file.name, files: [file]]
        result.add(pack)
      }
      else {
        topLevelFilesPack.files.add(file)
      }
    }

    result
  }

  static installerImages() {
    if (Pom.isEnterprise()) {
      ["enterprise-1.jpg", "enterprise-2.jpg", "enterprise-3.jpg",
       "enterprise-4.jpg", "enterprise-5.jpg", "enterprise-6.jpg",
       "enterprise-6.jpg"]
    }
    else {
      ["opensource-1.jpg", "opensource-2.jpg", "opensource-3.jpg",
       "opensource-4.jpg", "opensource-5.jpg", "opensource-5.jpg",
       "opensource-5.jpg"]
      
    }
  }
}


def xml = new MarkupBuilder(new FileWriter(Pom.property("izpack.install.xml")))

// Build the IzPack install.xml file with Groovy's MarkupBuilder
xml.installation(version: "1.0") {
  info {
    appname("Terracotta" + (Pom.isEnterprise() ? " Enterprise" : ""))
    appversion(project.version)
    appsubpath("Terracotta/${Pom.property('kit.name')}")
    url("http://www.terracotta.org/")
    javaversion("1.5")
  }

  guiprefs(width: "700", height: "530", resizable: "no") {
    modifier(key: "useHeadingPanel", value: "yes")
    modifier(key: "headingLineCount", value: "2")
    modifier(key: "headingFontSize", value: "1.5")
    modifier(key: "headingBackgroundColor", value: "0x00ffffff")
    modifier(key: "headingPanelCounter", value: "text")
    modifier(key: "headingPanelCounterPos", value: "inHeading")
  }

  locale {
    langpack(iso3: "eng")
  }

  xml.native(type: "izpack", name: "ShellLink.dll")
  xml.native(type: "izpack", name: "ShellLink_x64.dll")

  variables {
    variable(name: "DesktopShortcutCheckboxEnabled", value: "true")
  }

  resources {
    IzPack.installerImages().eachWithIndex { image, i ->
      res(id: "Installer.image.${i}", src: "../izpack/${image}")
    }
    res(id: "Heading.image", src: "../izpack/logo_rgb_padded.png")
    res(id: "LicencePanel.licence", src: Pom.property("terracotta.license"))
    res(id: "InfoPanel.info", src: "README.txt")
    res(src: "../izpack/shortcutSpec.xml", id: "shortcutSpec.xml")
    res(src: "../izpack/Unix_shortcutSpec.xml", id: "Unix_shortcutSpec.xml")
    res(id: "SimpleMessageFinishPanel.message", src: "../izpack/finish_message.txt")
  }

  panels {
    panel(classname: "HelloPanel", id: "hello")
    panel(classname: "LicencePanel", id: "licence")
    panel(classname: "TargetPanel", id: "target")
    panel(classname: "SummaryPanel", id: "summary")
    panel(classname: "InstallPanel", id: "install")
    panel(classname: "ShortcutPanel", id: "shortcuts")
    panel(classname: "SimpleMessageFinishPanel", id: "finish")
  }

  packs {
    IzPack.packs().each { p ->
      pack(name: p.name, required: "yes") {
        description(p.name)
        p.files.each { f ->
          file(src: f.name, targetdir: "\$INSTALL_PATH")
        }
      }
    }
  }
}
