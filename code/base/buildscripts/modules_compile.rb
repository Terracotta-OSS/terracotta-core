#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Adds methods to the BuildSubtree and BuildModule classes that give them the
# ability to compile themselves.

class BuildSubtree
  include BuildData
  
  # Compiles the given subtree. jvm_set is the set of JVMs available; the
  # subtree will look for one named 'compile-<version>', where <version> is
  # the value passed as :compiler_version in the options hash of the BuildSubtree
  # constructor. All the other parameters should be obvious; they're just the
  # obvious instances of the given classes.
  #
  # This method also copies all resources from the resources directory for this
  # subtree into the compiled-classes directory, if there are any resources.
  #
  # If this subtree doesn't really exist -- i.e., there's no source for it --
  # this method does nothing.
  def compile(jvm_set, build_results, ant, config_source, build_environment)
    if @source_exists
      build_results.classes_directory(self).ensure_directory

      jdk = compile_jdk

      if build_module.aspectj?
        puts "AspectJ #{build_module.name}/#{name}..."

        # Include the java.lang.System class to get access to Java
        # system properties
        javac_classpath = JavaSystem.getProperty('java.class.path')

        ant.java(
          :jvm => jdk.java.to_s,
          :fork => true,
          :resultproperty => 'aspectj.result',
          :failonerror => true,
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
          ant.arg(:value => '1.5')
          #~ ant.arg(:value => '-target')
          #~ ant.arg(:value => build_module.jdk.release)
          #~ ant.arg(:value => '-showWeaveInfo')
        end
      else
        puts "Compiling #{build_module.name}/#{name}... with #{jdk.short_description}"
        ant.javac(
          :destdir => build_results.classes_directory(self).to_s,
          :debug => true,
          :deprecation => false,
          :target => jdk.release,
          :source => jdk.release,
          :classpath => classpath(build_results, :full, :compile).to_s,
          :includeAntRuntime => false,
          :fork => true,
          :encoding => 'iso-8859-1',
          :executable => jdk.javac.to_s,
          :memoryMaximumSize => '256m') {

          ant.src(:path => source_root.to_s) { }
        }
        
        if Registry[:emma]
          prepare_emma_dir(build_results)
          
          unless %w(thirdparty thirdparty-api installer).include?(build_module.name) || 
              build_module.name =~ /test/ ||
              name =~ /test/
            ant.java(
              :jvm => jdk.java.to_s,
              :fork => true,
              :classpath => "#{Registry[:emma_lib]}",
              :classname => "emma" ) do
              ant.arg(:value => "instr")
              ant.arg(:value => "-ip")
              ant.arg(:value => build_results.classes_directory(self).to_s)
              ant.arg(:value => "-out")
              ant.arg(:value => "#{Registry[:emma_coverage_dir]}/coverage.em")
              ant.arg(:value => "-m")
              ant.arg(:value => "overwrite")
              ant.arg(:value => "-merge")
              ant.arg(:value => "yes")
              ant.arg(:value => "-ix")
              ant.arg(:value => "@#{Registry[:emma_home]}/excludes.txt")
            end
          end
        end
        
      end
    end

    if @resources_exists
      resources_dir = build_results.classes_directory(self).to_s
      ant.copy(:todir => resources_dir) do
        ant.filterset do
          ant.filter(:token => 'tc.version', :value => config_source['maven.version'])
        end
        ant.fileset(:dir => resource_root.to_s, :includes => '**/*.properties')
        ant.fileset(:dir => resource_root.to_s, :includes => '**/*.xml')
      end
      ant.copy(:todir => resources_dir) do
        ant.fileset(:dir => resource_root.to_s, :includes => '**/*', :excludes => '**/*.properties')
      end
    end
    
    create_data_file(config_source, build_results.classes_directory(self).to_s, :build_data)
  end

  private

  def prepare_emma_dir(build_results)      
    unless Registry[:emma_coverage_dir]
      Registry[:emma_coverage_dir] = File.join(build_results.build_dir.to_s, 'coverage')
      FileUtils.mkdir_p Registry[:emma_coverage_dir]
    end
  end
  
  # The JDK that should be used for compiling this subtree.
  def compile_jdk
    if name = Registry[:config_source]['jdk']
      Registry[:jvm_set][name]
    else
      build_module.jdk
    end
  end
end

class BuildModule
  include MavenConstants

  # Compiles the module. All this does is call BuildSubtree#compile on each of the module's
  # subtrees.
  def compile(jvm_set, build_results, ant, config_source, build_environment)
    @subtrees.each do |subtree|
      subtree.compile(jvm_set, build_results, ant, config_source, build_environment)
    end

    if self.module?
      module_info = create_module_jar(ant, build_results)
      if repo = config_source[MAVEN_REPO_CONFIG_KEY]
        maven = MavenDeploy.new(:group_id => MODULES_GROUP_ID,
          :repository_url => repo,
          :repository_id => config_source[MAVEN_REPO_ID_CONFIG_KEY],
          :snapshot => config_source[MAVEN_SNAPSHOT_CONFIG_KEY])
        maven.deploy_file(module_info.jarfile.to_s, module_info.artifact_id, module_info.version)
      end
    end
  end

  # Creates a JAR file for a pluggable module and stores it in build/modules.  Returns a
  # ModuleInfo object describing the module.
  def create_module_jar(ant, build_results)
    basedir  = build_results.classes_directory(subtree('src')).ensure_directory
    module_info = build_results.module_info(self)
    ant.jar(:destfile => module_info.jarfile.to_s, :basedir => basedir.to_s, :manifest => module_info.manifest.to_s)
    module_info
  end
end
