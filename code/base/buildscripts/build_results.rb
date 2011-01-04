#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# This is a very central class in our buildsystem -- it is the single class (see 'DRY' in The
# Pragmatic Programmer -- a very good book) that understands where everything is in the 'build'
# directory. <b>This is very important</b>; you may think it's annoying to pass around the
# BuildResults object, but the alternative is to spread out the knowledge of where stuff goes
# into a zillion different classes and then suffer abject pain the moment you have to change
# any of it.
#
# So, right now, absolutely _all_ knowledge about the structure of the 'build' directory is here,
# and, in this author's opinion, that is a very, very good thing.

# Represents the results of a build -- that is, the contents of the 'build' directory.
class BuildResults
  attr_reader :build_dir

  # Creates a new BuildResults instance. build_dir is the FilePath that points to the 'build' directory.
  def initialize(build_dir)
    @build_dir = build_dir
  end

  # Deletes absolutely all build results.
  def clean(ant)
    @build_dir.delete
  end

  # Returns a FilePath object referring to the directory in which Javadoc API
  # documentation is generated.
  def javadoc_directory
    FilePath.new(@build_dir, 'doc', 'api')
  end

  def module_root(the_module)
    FilePath.new(@build_dir, the_module.name)
  end

  # Returns the root of the classes hierarchy for the given build subtree -- that is, if you
  # want to use classes from the given subtree, this must be on your CLASSPATH.
  #
  # Returns a FilePath object.
  def classes_directory(subtree)
    FilePath.new(@build_dir, subtree.build_module.name, "#{subtree.name}.classes").ensure_directory
  end

  # Returns a FilePath representing the directory where shared boot JARs are stored.
  def shared_boot_jar_directory
    FilePath.new(@build_dir, "boot-jars")
  end

  # Given a subtree and a path (absolute or relative, starting from anywhere) to a .class
  # file, returns the fully-qualified name of the Java class that should reside in that file.
  def class_name_for_class_file(subtree, class_file)
    the_classes_directory = classes_directory(subtree)
    subpath_string = FilePath.new(class_file).relative_path_from(the_classes_directory).to_s
    subpath_string = $1 if subpath_string =~ /^(.*)\.class\s*$/
    subpath_string.gsub!("/", ".")
    subpath_string.gsub!("\\", ".")
    subpath_string
  end

  # The name of the file into which our build mechanism writes all the system properties that
  # should be set when running tests. This is written by 'check_prep', and read by the
  # Java TestConfigObject, in its static initializer block. Note that this is *quite* different
  # from the properties file that TestConfigObject reads to get the properties it returns in
  # general. The file returned here is *only* used to specify system properties that TestConfigObject
  # should set in the VM running the test, when running in Eclipse. (When running tests 'normally' --
  # via tcbuild -- we don't use this; we just set the properties directly on the command line when we
  # spawn the test's JVM.)
  def test_config_system_properties_file(module_set)
    FilePath.new(classes_directory_for_eclipse(module_set['common'].subtree('tests.base')), "test-system-properties.properties")
  end

  # The path to a properties file that indicates which tree we've run 'check_prep' for most recently.
  # This is read by Eric's Eclipse tool to figure out whether it needs to re-run 'check_prep' before
  # spinning up a test.
  def prepped_stamp_file(module_set)
    FilePath.new(classes_directory_for_eclipse(module_set['common'].subtree('tests.base')), "tests-prepared.properties")
  end

  # The directory that Eclipse puts the compiled classes of the given subtree into. This is set in
  # the Eclipse project by our Eclipse-project-generation mechanism, and used by check_prep to write
  # files out that can be read in as resources by our Eclipse code.
  def classes_directory_for_eclipse(subtree)
    FilePath.new(subtree.build_module.name, "build.eclipse", "#{subtree.name}.classes").ensure_directory
  end

  # The "tools home", which is the working directory we set when running various random Java processes
  # (like the boot JAR builder) that are run as part of the buildsystem.
  def tools_home
    FilePath.new(@build_dir, "homes", "tools").ensure_directory
  end

  # Creates a JAR archive of the entirety of the build-results directory to the given path.
  #
  # Currently, we exclude certain JARs from this archive to reduce its size. This is not ideal, because I'd
  # like to provide absolutely everything so that people can debug easily, but without this we get 300+ MByte
  # JARs created from certain monkey runs -- we can fill up a 400GB disk in a little under two days. Disk
  # is cheap, but not _that_ cheap.
  def archive_to(ant, path)
    if @build_dir.exist?
      path = FilePath.new(path)
      FilePath.new(path.directoryname).ensure_directory
      puts "Archiving entire build directory ('#@build_dir') to '#{path}'..."

      ant.jar(:destfile => path.to_s) do
        ant.fileset(:dir => @build_dir.to_s,
          :includes => "**/testrun*/**, **/tests_aggregation/**",
          :excludes => "**/*.jar,**/*.class,**/*.war,**/*.rar,**/var/**,**/repository/**,**/META-INF/**")
        ant.fileset(:dir => @build_dir.to_s,
          :includes => "**/boot-jars/**,**/build-config.local,**/war/*.war")
      end

      puts "Done archiving build."
    else
      puts "#{@build_dir.tos_s} does not exist to archive the testrun"
    end

  end

  # The directory to store module JAR files.
  def modules_home
    FilePath.new(@build_dir, "modules").ensure_directory
  end

  # The "server home", which is the working directory we set (and into which we copy certain files required
  # by the server) when we run the Terracotta Server directly from tcbuild. (This is not, and should not be, ever
  # used by tests.)
  def server_home
    FilePath.new(@build_dir, "homes", "server").ensure_directory
  end

  # The path to the XML file we write out with all the relevant properties of our build, so that monkeys
  # (and, most particularly, the monkey database) can pick it up and integrate it into the main CruiseControl
  # XML result log. This is how monkeys (and, thus, the monkey database) know things like which branch the
  # monkey build was from, when it was made, what JVMs it ran with, and so on.
  def build_information_file
    dir = FilePath.new(@build_dir, "cruisecontrol_integrated_xml_files").ensure_directory
    FilePath.new(dir, "build-information.xml")
  end

  # The directory into which we generate the XMLBeans source code that represents the Terracotta configuration
  # schema (.xsd files).
  def config_schema_generation_directory
    FilePath.new(@build_dir, "generated", "config-schema")
  end
    
    # The directory into which we generate the XMLBeans source code that represents the l1 reconnect properties from l2 (.xsd files)
    def l1_reconnect_properties_schema_generation_directory
        FilePath.new(@build_dir, "generated", "l1-reconnect-properties-schema")
    end

  def stats_config_schema_generation_directory
    FilePath.new(@build_dir, "generated", "stats-config-schema")
  end

  ModuleInfo = Struct.new("ModuleInfo", :artifact_id, :version, :jarfile, :manifest)

  # Returns a ModuleInfo object describing the given module, or nil if the module is not a
  # "config bundle"
  def module_info(build_module)
    if build_module.module?
      module_metainf_dir = FilePath.new(self.classes_directory(build_module.subtree('src')), 'META-INF')
      manifest = FilePath.new(module_metainf_dir, "MANIFEST.MF")
      module_version = extract_version_from_manifest(manifest.to_s).gsub(/\.SNAPSHOT/,'-SNAPSHOT').gsub(/\.patch/, '-patch')
      puts "module_version: #{module_version}"
      artifact = build_module.name
      output_dir = FilePath.new(self.modules_home, build_module.group_id.gsub(/\./, '/'),
        artifact, module_version).ensure_directory
      jarfile  = "#{artifact}-#{module_version}.jar"
      ModuleInfo.new(artifact, module_version, FilePath.new(output_dir, jarfile), manifest)
    else
      nil
    end
  end

  # Returns a FilePath for the JAR file produced for the given module, or nil if
  # the module does not produce a JAR file.
  def module_jar_file(build_module)
    module_info(build_module).jarfile
  end

  def artifacts_directory
    FilePath.new(@build_dir, "artifacts").ensure_directory
  end

  def artifacts_classes_directory
    FilePath.new(@build_dir, "artifacts-classes")
  end

  private

  def extract_version_from_manifest(manifest_file)
    bundle_version = YAML.load_file(manifest_file)['Bundle-Version']
    bundle_version ? bundle_version.strip : ''
  end
end
