#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module BundledComponents
  def bundled_components(name, directory, spec)
    add_binaries(spec)
    @static_resources.supported_platforms(@build_environment).each do |platform|
      srcdir = FilePath.new(@static_resources.skeletons_directory, name, platform).to_s
      if File.directory?(srcdir)
        non_native = @build_environment.is_unix_like? ? ['*.bat', '*.cmd', '*.exe'] : ['*.sh']
        destdir    = FilePath.new(product_directory, directory).ensure_directory
        ant.copy(:todir => destdir.to_s) do
          ant.fileset(:dir => srcdir, :excludes => "**/.svn/**, **/.*, **/*/#{non_native.join(', **/*/')}")
        end
      end
    end
    add_dso_bootjar(spec)
    add_module_bootjars(spec)
    add_documentations(spec)
  end

  def add_binaries(component, libdir=libpath(component), destdir=libpath(component))
    runtime_classes_dir = FilePath.new(@distribution_results.build_dir, 'tmp').ensure_directory
    (component[:modules] || []).each do |a_module|
      name = a_module
      exclude_native_runtime_libraries = false
      exclude_runtime_libraries        = false
      if a_module.instance_of?(Hash)
        name                             = a_module.keys.first
        exclude_native_runtime_libraries = a_module.values.first['exclude-native-runtime-libraries']
        exclude_runtime_libraries        = a_module.values.first['exclude-runtime-libraries']
      end
      a_module = @module_set[name]
      a_module.subtree('src').copy_native_runtime_libraries(libdir, ant, @build_environment) unless exclude_native_runtime_libraries
      a_module.subtree('src').copy_runtime_libraries(libdir, ant)                            unless exclude_runtime_libraries
      a_module.subtree('src').copy_classes(@build_results, runtime_classes_dir, ant)
    end

    jarfile = FilePath.new(destdir, "tc.jar")
    if File.exist?(libdir.to_s)
      ant.jar(:destfile => jarfile.to_s, :basedir => runtime_classes_dir.to_s) do
        classpath = ''
        libfiles  = Dir.entries(libdir.to_s).delete_if { |item| /\.jar$/ !~ item }
        classpath << "#{libfiles.first} "
        libfiles[1..-2].each { |item| classpath << "#{item} " }
        classpath << "#{libfiles.last}"
        ant.manifest { ant.attribute(:name => 'Class-Path', :value => classpath) }
      end
    end
    runtime_classes_dir.delete
  end

  def add_dso_bootjar(component, destdir=libpath(component))
    bootjar_spec = component[:bootjar]
    unless bootjar_spec.nil?
      bootjar_spec['compiler_versions'].each do |version|
        jvm = JVM.from_config(@platform, @config_source,
          "the specified JVM for Java #{version}", "#{version}.0_0",
          "#{version}.999_999",
          "JAVA_HOME_#{version.to_s.gsub(/\./, '')}")
        bootjar_dir = FilePath.new(File.dirname(destdir.to_s), *bootjar_spec['install_directory'].split('/')).ensure_directory
        bootjar     = BootJar.new(
          @build_results,
          jvm,
          bootjar_dir,
          @module_set,
          ant,
          @platform,
          @static_resources.dso_boot_jar_config_file.to_s)
        bootjar.ensure_created
      end
    end
  end

  def add_module_bootjars(component, destdir=libpath(component))
    (component[:module_bootjars] || []).each do |bootjar|
      runtime_classes_dir = FilePath.new(@distribution_results.build_dir, 'tmp').ensure_directory
      bootjar.keys.each do |name|
        bootjar[name]['modules'].each do |module_name|
          a_module  = @module_set[module_name]
          a_module.subtree('src').copy_classes(@build_results, runtime_classes_dir, ant)
        end
        libdir  = FilePath.new(File.dirname(destdir.to_s), *bootjar[name]['install_directory'].split('/')).ensure_directory
        jarfile = FilePath.new(libdir, "#{name}.jar")
        ant.jar(:destfile => jarfile.to_s, :basedir => runtime_classes_dir.to_s)
      end
      runtime_classes_dir.delete
    end
  end

  def add_documentations(component)
    (component[:documentations] || {}).keys.each do |document_set|
      document_set.each do |name|
        component[:documentations][name].each do |document|
          @static_resources.supported_documentation_formats.each do |format|
            srcfile = FilePath.new(@static_resources.documentations_directory, *(name.split('/') << "#{document}.#{format}")).to_s
            destdir = docspath(component)
            case format
            when /html\.zip/
              # This has been moved to the new wiki
              # ant.unzip(:dest => destdir.ensure_directory.to_s, :src => srcfile)
            else
              ant.copy(:todir => destdir.ensure_directory.to_s, :file => srcfile)
            end if File.exist?(srcfile)
          end
        end
      end
    end
  end
end
