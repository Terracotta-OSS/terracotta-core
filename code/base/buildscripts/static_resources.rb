#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
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

    def build_config_directory
        FilePath.new(@root_directory, 'buildconfig')
    end

    def distribution_config_directory(flavor)
      FilePath.new(source_root(flavor), 'buildconfig', 'distribution')
    end

    def enterprise_modules_file
      FilePath.new(@root_directory, '..', '..', '..', 'code', 'base', 'modules.def.yml')
    end

    def ia_project_directory(flavor)
      (flavor !~ /opensource/i) ? FilePath.new(@root_directory, '..', '..', '..', 'kits', 'source', 'installer') :
                                  FilePath.new(@root_directory, '..', '..',  'kits', 'source', 'installer')
    end

    def jrefactory_config_directory
        FilePath.new(@root_directory, "buildconfig")
    end

    # Where does the default Log4J properties file live?
    def log4j_properties_file
        FilePath.new(@root_directory, ".tc.dev.log4j.properties")
    end

    # Where does the compiled version of the configuration schema live (a JAR, that is)?
    def compiled_config_schema_jar(module_set)
        out = nil

        module_set['common'].subtree('src').subtree_only_library_roots(:runtime).each do |root|
            test = FilePath.new(root, "tcconfig-xmlbeans-generated.jar")
            out = test if FileTest.file?(test.to_s)
        end

        assert { not out.nil? }

        out
    end

    # Where do the .xsd files for configuration live?
    def config_schema_source_directory(module_set)
        FilePath.new(module_set['common'].subtree('src').resource_root, "com", "tc", "config", "schema")
    end

    # Where do the .xsdconfig files for configuration live?
    def config_schema_config_directory(module_set)
        FilePath.new(module_set['common'].subtree('src').resource_root, "com", "tc", "config", "schema-config")
    end

    def demos_directory
        FilePath.new(@root_directory, '..', 'demos')
    end

    def vendors_url
        default_url = "http://download.terracotta.org/bundled-vendors"
        ENV["TC_VENDORS_URL"].nil? ? default_url : ENV["TC_VENDORS_URL"]
    end

    def jre_url
      default_url = "http://download.terracotta.org/bundled-jre/sun"
      ENV["TC_JRE_URL"].nil? ? default_url : ENV["TC_JRE_URL"]
    end

    def docflex_home
      ENV["DOCFLEX_HOME"]
    end

    def templates_directory
        FilePath.new(@root_directory, '..', '..', 'kits', 'source', 'templates')
    end

    def skeletons_directory
        FilePath.new(@root_directory, '..', '..', 'kits', 'skeletons')
    end

    def documentations_directory
        FilePath.new(@root_directory, '..', '..', 'kits', 'source', 'docs')
    end

    def notices_directory
      FilePath.new(documentations_directory, 'distribute', 'notices')
    end

    # Where does the Ant executable live?
    def ant_script
        fail("ANT_HOME is not defined. Please set env variable ANT_HOME to Apache Ant 1.6.5 or later.") if ENV['ANT_HOME'].nil?
        ant_script = FilePath.new(ENV['ANT_HOME'], 'bin', 'ant').canonicalize.to_s
    end

    def supported_documentation_formats
        ['pdf', 'html', 'html.zip', 'txt', 'xml']
    end

    def supported_platforms(build_environment)
        ['common', build_environment.os_family.downcase, build_environment.os_type(:nice).downcase]
    end

    # 2006-07-19 andrew -- HACK HACK HACK. This is ONLY in support of 'tc.install_dir' in
    # BuildSubtree.create_build_configuration_file. Once that's gone, we should remove it
    # here, too.
    def root_dir
        @root_directory
    end
end
