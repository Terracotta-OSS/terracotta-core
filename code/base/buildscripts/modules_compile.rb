#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Adds methods to the BuildSubtree and BuildModule classes that give them the
# ability to compile themselves.

class BuildSubtree
    # Compiles the given subtree. jvm_set is the set of JVMs available; the
    # subtree will look for one named 'compile-<version>', where <version> is
    # the value passed as :compiler_version in the options hash of the BuildSubtree
    # constructor. All the other parameters should be obvious; they're just the
    # obvious instances of the given classes.
    #
    # This method also copies all resources from the resources directory for this
    # subtree into the compiled-classes directory, if there are any resources.
    # It further creates a 'build-data.txt' file in the root of the compiled-
    # classes directory that contains information about when and where the
    # compiled classes were created.
    #
    # If this subtree doesn't really exist -- i.e., there's no source for it --
    # this method does nothing.
    def compile(jvm_set, build_results, ant, config_source, build_environment)
        if @source_exists
            build_results.classes_directory(self).ensure_directory

            jvm = jvm_set['compile-%s' % build_module.compiler_version]
            
            if build_module.aspectj
                puts "AspectJ %s/%s..." % [ build_module.name, name ]

                # puts "-- %s" % [ ant.get_ant_property('env.CLASSPATH') ]

#                ant.taskdef(
#                    :name => "iajc", 
#                    :classname => "org.aspectj.tools.ant.taskdefs.AjcTask")

                # puts "  AJ %s" % [ aspect_libraries(:compile) ]
                # puts "  CP %s" % [ classpath(build_results, :full, :compile).to_s ]

                # Include the java.lang.System class to get access to Java
                # system properties
                include_class('java.lang.System')
                javac_classpath = System.getProperty('java.class.path')

                ant.java(
                   :jvm => jvm.java.to_s,
                   :fork => true,
                   :maxmemory => '256m',
                   :classpath => javac_classpath,
                   :classname => "org.aspectj.tools.ajc.Main") do
                     ant.arg(:value => '-sourceroots')
                     ant.arg(:value => source_root.to_s)
                     ant.arg(:value => '-aspectpath')
                     ant.arg(:value => aspect_libraries(:compile).to_s)
                     ant.arg(:value => '-classpath')
                     ant.arg(:value => classpath(build_results, :full, :compile).to_s)
                     ant.arg(:value => '-d')
                     ant.arg(:value => build_results.classes_directory(self).to_s)
                     ant.arg(:value => '-source')
                     ant.arg(:value => build_module.compiler_version)
                     ant.arg(:value => '-target')
                     ant.arg(:value => build_module.compiler_version)
                     ant.arg(:value => '-noExit')
                     ant.arg(:value => '-showWeaveInfo')
                end

#                ant.iajc(
#                    :srcdir => source_root.to_s,
#                    :aspectPath => aspect_libraries(:compile).to_s,
#                    :classpath => classpath(build_results, :full, :compile).to_s,
#                    :destdir => build_results.classes_directory(self).to_s,
#
#                    # :debug => true,
#                    :deprecation => false,
#                    :target => build_module.compiler_version,
#                    :source => build_module.compiler_version,
#                    # :includeAntRuntime => false,
#                    :fork => true,
#                    # :encoding => 'iso-8859-1',
#                    # :executable => compile_javac(jvm_set).to_s,
#                    :maxmem => '256m')
            
            else
                puts "Compiling %s/%s..." % [ build_module.name, name ]
                
                ant.javac(
                    :destdir => build_results.classes_directory(self).to_s,
                    :debug => true,
                    :deprecation => false,
                    :target => build_module.compiler_version,
                    :source => build_module.compiler_version,
                    :classpath => classpath(build_results, :full, :compile).to_s,
                    :includeAntRuntime => false,
                    :fork => true,
                    :encoding => 'iso-8859-1',
                    :executable => jvm.javac.to_s,
                    :memoryMaximumSize => '256m') {
                
                    ant.src(:path => source_root.to_s) { }
                }
            end
            
            if @resources_exists
                ant.copy(:todir => build_results.classes_directory(self).to_s) {
                    ant.fileset(:dir => resource_root.to_s, :includes => '**/*')
                }
            end
            
            create_build_data(config_source, build_results, build_environment)
        else
            # puts "(no #{source_root})"
        end
    end

    protected
    # Where should we put our build-data file?
    def build_data_file(build_results)
        FilePath.new(build_results.classes_directory(self), "build-data.txt")
    end

    private
    # Which compiler should we use for this subtree?
    def compile_javac(jvm_set)
        jvm_set['compile-%s' % build_module.compiler_version].javac
    end
    
    # Creates a 'build data' file at the given location, putting into it a number
    # of properties that specify when, where, and how the code in it was compiled.
    def create_build_data(config_source, build_results, build_environment)
        File.open(build_data_file(build_results).to_s, "w") do |file|
            file << "terracotta.build.productname=terracotta\n" 
            file << "terracotta.build.version=%s\n" % build_environment.specified_build_version
            file << "terracotta.build.host=%s\n" % build_environment.build_hostname
            file << "terracotta.build.user=%s\n" % build_environment.build_username
            file << "terracotta.build.timestamp=%s\n" % build_environment.build_timestamp.strftime('%Y%m%d-%H%m%S')
            file << "terracotta.build.revision=%s\n" % build_environment.current_revision
            file << "terracotta.build.change-tag=%s\n" % build_environment.current_revision_tag
            file << "terracotta.build.branch=%s\n" % build_environment.current_branch
        end
    end
end

class BuildModule
    # Compiles the module. All this does is call BuildSubtree#compile on each of the module's
    # subtrees.
    def compile(*args)
        @subtrees.each { |subtree| subtree.compile(*args) }
    end
end
