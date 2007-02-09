#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Contains classes and methods having to do with creating boot JARs based on various JVMs, etc.

# Adds a method to BuildSubtree indicating where the boot JAR configuration file for the
# given subtree is; falls back to a static copy if none is present for this boot JAR.
class BuildSubtree
    # Returns the Terracotta configuration file that should be used to build the boot JAR
    # for the given subtree. This will be _module_/_subtree_.boot-jar-config.xml if it exists,
    # or static_resources.dso_boot_jar_config_file otherwise.
    def boot_jar_config_file(static_resources)
        out = FilePath.new(build_module.root, "%s.boot-jar-config.xml" % name)
        out = static_resources.dso_boot_jar_config_file unless FileTest.file?(out.to_s)
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
    def initialize(build_results, jvm, directory, module_set, ant, platform, config_file)
        @build_results = build_results
        @jvm           = jvm
        @directory     = directory
        @path          = nil
        @module_set    = module_set
        @ant           = ant
        @platform      = platform
        @config_file   = config_file
        @created       = false
    end

    # What's the fully-qualified path of this boot JAR? Returns a FilePath object. (This is
    # useful because the exact name of the boot JAR depends on the exact JVM you're running
    # it with; this uses the Terracotta Java code to compute the name, ensuring that we
    # don't duplicate the algorithm -- almost certainly incorrectly -- here.)
    def path
        # We cache this, since it can be fairly expensive to run the command to figure out
        # what this should be.
        if @path.nil?
            outputproperty, errorproperty = @platform.next_output_properties
            @ant.java(
            :classname   => 'com.tc.object.tools.BootJarSignature',
            :classpath   => @module_set['dso-l1'].subtree('src').classpath(@build_results, :full, :runtime).to_s,
            :jvm         => @jvm.java,
            :fork        => true,
            :failonerror => true,
            :dir         => @build_results.tools_home.to_s) do
                @ant.redirector(:outputproperty => outputproperty, :alwayslog => true)
            end

            out   = @ant.get_ant_property(outputproperty)
            @path = FilePath.new(@directory, out)
        end

        @path
    end

    # Make sure there's a boot JAR at the given location. This <b>will delete any existing file
    # at the specified location</b>! Be careful. This will only create this boot JAR once for
    # a given instance of this object, so you don't have to worry about repeating work.
    def ensure_created
        unless @created
            puts("Creating boot JAR with #{@jvm}")
            File.delete(path.to_s) if FileTest.exist?(path.to_s)
            @ant.java(
            :classname   => 'com.tc.object.tools.BootJarTool',
            :classpath   => @module_set['dso-tests-jdk15'].subtree('src').classpath(@build_results, :full, :runtime).to_s,
            :jvm         => @jvm.java,
            :fork        => true,
            :failonerror => true,
            :dir         => @build_results.tools_home.to_s) do
                @ant.arg(:value => '-o')
                @ant.arg(:value => @directory.ensure_directory)
                @ant.arg(:value => '-f')
                @ant.arg(:value => @config_file)
                @ant.arg(:value => '-q')
            end

            @created = true
        end
        self
    end

    # Have we created this boot JAR yet?
    def exist?
        @created
    end
end
