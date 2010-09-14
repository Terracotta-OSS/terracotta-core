#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# An object that knows about all of the static resources in the buildsystem -- that is,
# it understands where a bunch of support files (that the buildsystem needs to do its
# job) live. Like BuildResults, it's much cleaner and more maintainable to have these
# all in one place rather than spread out all over the buildsystem. (No, really. You will
# appreciate this later. ;)
class StaticResources
  # Creates a new instance; root_directory is the root directory of the project (code/base/).
  def initialize(root_directory)
    @root_directory = root_directory
  end

  # Where does the Terracotta config file that we should use to build the DSO boot JAR
  # (by default) live?
  def dso_boot_jar_config_file
    FilePath.new(@root_directory, "buildscripts", "templates", "dso-support", "tc-config.xml")
  end

  # What Terracotta configuration file should we use when we spawn, e.g., JUnit tests with
  # a boot JAR in the bootclasspath?
  def dso_test_runtime_config_file
    dso_boot_jar_config_file
  end

  # A directory that contains a set of files that should be copied to a directory in order
  # to make it a valid Terracotta home (e.g., so that it can run the Terracotta server).
  def terracotta_home_template_directory
    FilePath.new(@root_directory, "buildscripts", "templates", "terracotta-home")
  end

  def extensions_directory
    FilePath.new(@root_directory, "buildscripts", "extensions")
  end

  # Returns a FilePath object referring to the directory where library
  # dependencies are installed.
  def lib_dependencies_directory
    FilePath.new(@root_directory, "dependencies", "lib")
  end

  def kit_builder_scripts
    FilePath.new(@root_directory, "buildscripts", "postscripts")
  end

  def source_root(flavor)
    (flavor !~ /opensource/i) ? FilePath.new(@root_directory, '..', '..', '..', 'code', 'base') : @root_directory
  end

  def build_cache_directory
    FilePath.new(@root_directory, '.tc-build-cache')
  end

  def global_cache_directory
    FilePath.new(ENV['HOME'], '.tc', 'cache').ensure_directory.canonicalize
  end

  def build_config_directory
    FilePath.new(@root_directory, 'buildconfig')
  end

  def global_filters_file
    FilePath.new(build_config_directory, 'global-filters.def.yml')
  end

  def distribution_config_directory
    FilePath.new(@root_directory, 'buildconfig', 'distribution')
  end

  def izpack_directory
    FilePath.new(@root_directory, 'buildconfig', 'izpack')
  end

  def izpack_resources_directory
    FilePath.new(izpack_directory, 'resources')
  end

  def izpack_installer_template
    FilePath.new(izpack_directory, 'izpack-installer-template.xml')
  end

  def izpack_shortcuts_template
    FilePath.new(izpack_directory, 'izpack-shortcuts.def.yml')
  end

  def enterprise_modules_file
    FilePath.new(@root_directory, '..', '..', '..', 'code', 'base', 'modules.def.yml')
  end

  def kits_directory
    FilePath.new(@root_directory, '..', '..', 'kits')
  end

  def jrefactory_config_directory
    FilePath.new(@root_directory, "buildconfig")
  end

  # Where does the default Log4J properties file live?
  def log4j_properties_file
    FilePath.new(@root_directory, ".tc.dev.log4j.properties")
  end

  def demos_directory
    FilePath.new(@root_directory, '..', 'demos')
  end

  def vendors_url
    default_url = "http://download.terracotta.org/bundled-vendors"
    ENV["TC_VENDORS_URL"].nil? ? default_url : ENV["TC_VENDORS_URL"]
  end

  def docflex_home
    ENV["DOCFLEX_HOME"]
  end

  def templates_directory
    FilePath.new(kits_directory, 'source', 'templates')
  end

  def skeletons_directory
    FilePath.new(kits_directory, 'skeletons')
  end

  def enterprise_skeletons_directory
    FilePath.new(@root_directory, '..', '..', '..', 'kits', 'skeletons')
  end

  def documentations_directory
    FilePath.new(kits_directory, 'source', 'docs')
  end

  def notices_directory
    FilePath.new(documentations_directory, 'notices')
  end

  def license_file
    FilePath.new(notices_directory, 'license.txt')
  end

  def thirdparty_license_file
    FilePath.new(notices_directory, 'thirdpartylicenses.txt')
  end

  # Where does the Ant executable live?
  def ant_script
    fail("ANT_HOME is not defined. Please set env variable ANT_HOME to Apache Ant 1.6.5 or later.") if ENV['ANT_HOME'].nil?
    ant_script = FilePath.new(ENV['ANT_HOME'], 'bin', 'ant').canonicalize.to_s
    if ENV['OS'] =~ /Windows/i
      ant_script += ".bat"
      ant_script.gsub!(/\\/, "/")
    end
    ant_script
  end

  def supported_documentation_formats
    ['pdf', 'html', 'html.zip', 'txt', 'xml', 'TXT']
  end

  def supported_platforms(build_environment)
    ['common', build_environment.os_family.downcase, build_environment.os_type(:nice).downcase]
  end

  def config_schema_source_directory(module_set)
    FilePath.new(module_set['common'].subtree('src').resource_root, "com", "tc", "config", "schema")
  end

  def external_projects_directory
    FilePath.new(@root_directory, 'external')
  end

  # 2006-07-19 andrew -- HACK HACK HACK. This is ONLY in support of 'tc.install_dir' in
  # BuildSubtree.create_build_configuration_file. Once that's gone, we should remove it
  # here, too.
  def root_dir
    @root_directory
  end
end
