#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Contains classes and methods having to do with creating boot JARs based on various JVMs, etc.

# Adds a method to BuildSubtree indicating where the boot JAR configuration file for the
# given subtree is; falls back to a static copy if none is present for this boot JAR.
class BuildSubtree
  BOOT_JAR_CONFIG_FILE_BASENAME = '.boot-jar-config.xml'

  # Returns the Terracotta configuration file that should be used to build the boot JAR
  # for the given subtree. This will be _module_/_subtree_.boot-jar-config.xml if it exists,
  # or static_resources.dso_boot_jar_config_file otherwise.
  def boot_jar_config_file(static_resources)
    jdktype = @build_module.jdk.actual_type
    jdkrelease = @build_module.jdk.release
    out = FilePath.new(build_module.root, "#{name}.#{jdktype}-#{jdkrelease}#{BOOT_JAR_CONFIG_FILE_BASENAME}")
    if !FileTest.file?(out.to_s)
      out = FilePath.new(build_module.root, "#{name}.#{jdktype}#{BOOT_JAR_CONFIG_FILE_BASENAME}")
      if !FileTest.file?(out.to_s)
        out = FilePath.new(build_module.root, "#{name}#{BOOT_JAR_CONFIG_FILE_BASENAME}")
        if !FileTest.file?(out.to_s)
          out = static_resources.dso_boot_jar_config_file unless FileTest.file?(out.to_s)
        end
      end
    end
    out
  end
end

# Represents a boot JAR. Lets you create the boot JAR, figure out the exact name of it (since
# it's based on the JVM that it's for), and see if it exists yet.
class BootJar

  # Creates a new object representing a boot JAR for the given JVM, living in the given
  # directory, built with the given configuration file. No files are actually touched or
  # even examined in this constructor -- you just get back the object; you have to call
  # #ensure_created to actually get anything built.
  #
  # jvm is the object (of class JVM) representing the JVM you want to use; directory is
  # the FilePath representing the directory you want the boot JAR stored in. You also need
  # to pass in the build results (so we can get at the Terracotta classes that are used
  # to find and build boot JARs), the module set (ditto), ant (so we can actually run Java
  # to build the boot JAR), and the platform (ditto).
  def initialize(jvm, directory, module_set, config_file)
    @build_results = Registry[:build_results]
    @jvm           = jvm
    @directory     = directory
    @module_set    = module_set
    @config_file   = config_file
    @path          = nil

    @ant = Registry[:ant]
    @platform = Registry[:platform]
    @static_resources = Registry[:static_resources]
    @extra_classpath = []
  end

  def add_extra_classpath(path)
    @extra_classpath << path
  end

  # What's the fully-qualified path of this boot JAR? Returns a FilePath object. (This is
  # useful because the exact name of the boot JAR depends on the exact JVM you're running
  # it with; this uses the Terracotta Java code to compute the name, ensuring that we
  # don't duplicate the algorithm -- almost certainly incorrectly -- here.)
  def path
    # We cache this, since it can be fairly expensive to run the command to figure out
    # what this should be.
    unless @path
      outputproperty, errorproperty = @platform.next_output_properties
      classpath = @module_set['dso-l1'].subtree('src').classpath(@build_results, :full, :runtime).to_s
      classpath = PathSet.new(classpath, Registry[:emma_lib]).to_s if Registry[:emma]
      @ant.java(:classname   => 'com.tc.object.tools.BootJarSignature',
        :classpath   => classpath,
        :jvm         => @jvm.java,
        :fork        => true,
        :failonerror => true,
        :dir         => @build_results.tools_home.to_s) do
        @ant.redirector(:outputproperty => outputproperty, :alwayslog => true)
      end

      # Parse the output in case the JVM prints out additional messages, only the line
      # that contains the actual boot jar name will be used.
      # On MacOSX the JVM sometimes prints "HotSpot not at correct virtual address.
      # Sharing disabled." for instance. Without parsing the output, the name of the
      # bootjar file will be wrong.
      rawout   = @ant.get_ant_property(outputproperty)
      out      = rawout
      rawout.each do |line|
        if line =~ /^dso-boot-/
          out = line
          break
        end
      end
      @path = FilePath.new(@directory, out)
    end

    @path
  end

  # Make sure there's a boot JAR at the given location.  To delete any existing file
  # at the specified location, pass :delete_existing => true as part of the options hash.
  # This will only create this boot JAR once for a given instance of this object, so you
  # don't have to worry about repeating work.
  def ensure_created(options = {})
    File.delete(path.to_s) if options[:delete_existing] && exist?
    unless exist?
      classpath = @module_set['dso-system-tests'].subtree('src').classpath(@build_results, :full, :runtime)
      classpath = PathSet.new(classpath, Registry[:emma_lib]).to_s if Registry[:emma]
      @extra_classpath.each do |path|
        classpath = PathSet.new(classpath, path)
      end
      puts("Creating boot JAR with: #{@jvm} and config file: #{@config_file}")

      sysproperties = {
        PropertyNames::TC_BASE_DIR => @static_resources.root_dir.to_s,
        PropertyNames::MODULES_URL => @build_results.modules_home.to_s
      }

      begin
        @ant.java(:classname   => 'com.tc.object.tools.BootJarTool',
          :classpath   => classpath.to_s,
          :jvm         => @jvm.java,
          :fork        => true,
          :failonerror => true,
          :dir         => @build_results.tools_home.to_s) do

          sysproperties.each do |name, value|
            @ant.sysproperty(:key => name.xml_escape(true),
              :value => value.xml_escape(true))
          end

          @ant.arg(:value => 'make')
          @ant.arg(:value => '-o')
          @ant.arg(:value => @directory.ensure_directory)
          @ant.arg(:value => '-f')
          @ant.arg(:value => @config_file)
          @ant.arg(:value => '-q')
        end
      rescue
        raise("Bootjar creation failed")
      end
    end
    self
  end

  # Does the boot JAR exist?
  def exist?
    File.exist?(path.to_s)
  end
end


## Subtitute for BootJar class when user wants
## to use their own bootjar, specified by "boot_jar_path="
## on command line
class UserBootJar
  def initialize(path)
    @boot_jar_path = path
    fail("Can't find bootjar defined by boot_jar_path [#{@boot_jar_path}]") unless File.exist?(@boot_jar_path)
  end

  def path
    @boot_jar_path
  end

  def exist?
    @boot_jar_path
  end
end
