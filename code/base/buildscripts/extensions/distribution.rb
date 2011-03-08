#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  include MavenConstants
  include BuildData

  # assemble the kit for the product code supplied
  def dist(product_code='DSO', flavor=OPENSOURCE)
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile
    call_actions :__assemble
  end

  # Assemble a kit just like 'dist', but then selectively extract elements from
  # the kit and bundle into a patch tarball.
  def patch(product_code = 'DSO', flavor = OPENSOURCE)
    @no_demo = true
    $patch = true
    
    patch_descriptor = get_patch_descriptor

    patch_level = config_source['level'] || patch_descriptor['level']
    $XXX_patch_level = patch_level

    if config_source[MAVEN_REPO_CONFIG_KEY]
      @internal_config_source[MAVEN_CLASSIFIER_CONFIG_KEY] = "patch#{patch_level}"
    end

    dist(product_code, flavor)

    begin
      # Warn if patch level is not a positive integer
      i = Integer(patch_level)
      raise("not a positive integer") if i < 0
    rescue
      loud_message("WARNING: patch level is not a positive integer")
    end

    puts("Building patch level #{patch_level}")

    # executables will get different permissions in patch tar
    patch_files = Array.new
    patch_bin_files = Array.new

    Dir.chdir(product_directory.to_s) do
      # Make sure patch-data.txt and RELEASE-NOTES.txt are included in the patch
      patch_files << create_patch_data(patch_level, @config_source, FilePath.new('lib', 'resources'))
      patch_files << 'RELEASE-NOTES.txt'

      patch_descriptor['files'].each do |file|
        raise("Patch may not include build-data.txt") if file =~ /build-data\.txt$/

        path = file.to_s
        unless File.readable?(path)
          raise("Patch descriptor file references non-existant file: #{path}")
        end

        if file =~ /\.sh$|\.bat$|\.exe$|\.dll$|\/bin\/|\/libexec\//
          patch_bin_files << path
        else 
          patch_files << path
        end
      end

      patch_file_name = File.basename(Dir.pwd) + "-patch#{patch_level}.tar.gz"
      patch_file = FilePath.new(self.dist_directory, patch_file_name)
      ant.tar(:destfile => patch_file.to_s, :compression => 'gzip', :longfile => 'gnu') do
        ant.tarfileset(:dir => Dir.pwd, :includes => patch_files.join(','))
        unless patch_bin_files.empty?
          ant.tarfileset(:dir => Dir.pwd,
            :includes => patch_bin_files.join(','),
            :mode => 755)
        end
      end
      
      if config_source['target']
        ant.copy(:file => patch_file.to_s, :todir => config_source['target'], :verbose => true)
      end
      
    end
  end

  def dist_all(flavor=OPENSOURCE)
    @flavor = flavor.downcase
    depends :init, :compile
    product_definition_files(flavor).each do |product_definition_file|
      @product_code = product_code(product_definition_file)
      @flavor       = flavor
      call_actions :__assemble
      srcdir  = product_directory.to_s
      destdir = FilePath.new(@distribution_results.archive_dir.ensure_directory, package_filename).to_s
      ant.move(:file => srcdir, :todir => destdir)
    end
  end

  def dist_maven
    mvn_install(OPENSOURCE)
  end

  def dist_maven_ee
    mvn_install(ENTERPRISE)
  end

  def dist_maven_all
    mvn_install(OPENSOURCE)
    @flavor = ENTERPRISE
    load_config
    mvn_install(ENTERPRISE)
  end

  def dist_dev(product_code = 'DSO', flavor = OPENSOURCE)
    @internal_config_source[MAVEN_USE_LOCAL_REPO_KEY] = 'true'
    if flavor == ENTERPRISE then dist_maven_all else dist_maven end
    build_external
    
    @internal_config_source['flavor'] = flavor
    dist(product_code, flavor)
  end
  
  # assemble and package the kits  for the product code supplied
  def create_package(product_code='DSO', flavor=OPENSOURCE)
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile
    call_actions :__assemble, :__package
  end

  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory)
  def create_all_packages(flavor=OPENSOURCE)
    @flavor = flavor.downcase

    depends :init, :compile
    srcdir        = @static_resources.distribution_config_directory.canonicalize.to_s
    product_codes = Dir.entries(srcdir).delete_if { |entry| (/\-(#{flavor})\.def\.yml$/i !~ entry) || (/^x\-/i =~ entry) }
    product_codes.each do |product_code|
      @product_code = product_code.sub(/\-.*$/, '')
      @flavor       = flavor
      call_actions :__assemble, :__package
      __publish @distribution_results.archive_dir.ensure_directory
    end
  end

  # assemble, package, and publish the kit for the product code supplied
  def publish_package(product_code='DSO', flavor=OPENSOURCE)
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile
    call_actions :__assemble, :__package, :__publish
  end

  # assemble, package, and publish the kit for the product code supplied
  def publish_packages(product_codes, flavor)
    depends :init, :compile
    product_codes.each do |product_code|
      @product_code = product_code
      @flavor = flavor.downcase
      @internal_config_source['flavor'] = @flavor
      call_actions :__assemble, :__package, :__publish
    end
  end

  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory)
  def publish_all_packages(flavor=OPENSOURCE)
    @flavor = flavor.downcase

    depends :init, :compile
    srcdir        = @static_resources.distribution_config_directory.canonicalize.to_s
    product_codes = Dir.entries(srcdir).delete_if { |entry| (/\-(#{flavor})\.def\.yml$/i !~ entry) || (/^x\-/i =~ entry) }
    product_codes.each do |product_code|
      @product_code = product_code.sub(/\-.*$/, '')
      @flavor       = flavor
      call_actions :__assemble, :__package, :__publish
    end
  end

  # HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK
  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory) for the opensource version
  # of the product
  def publish_opensource_packages
    publish_all_packages(OPENSOURCE)
  end

  # HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK
  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory) for the enterprise version
  # of the product
  def publish_enterprise_packages
    publish_all_packages(ENTERPRISE)
  end

  # build the JAR files for the product code supplied without assembling a full kit
  def dist_jars(product_code='DSO', component_name='common', flavor=OPENSOURCE)
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile, :load_config
    @flavor = flavor.downcase
    component = get_spec(:bundled_components, []).find { |component| /^#{component_name}$/i =~ component[:name] }
    libdir    = FilePath.new(@build_results.build_dir, 'tmp').ensure_directory
    destdir   = FilePath.new(@build_results.artifacts_directory, @flavor)
    add_binaries(component, libdir, destdir, false)
    libdir.delete

    add_module_packages(component, destdir)
    create_data_file(@config_source, File.join(destdir.to_s, 'resources'), :build_data, @build_environment.edition(flavor))
    
    # generate patch-data.txt if operating under a patch branch
    if @build_environment.patch_branch?
      patch_descriptor = get_patch_descriptor
      patch_level = @config_source['level'] || patch_descriptor['level']
      create_patch_data(patch_level, @config_source, File.join(destdir.to_s, 'resources'))
    end
  end

  def mvn_install(flavor=OPENSOURCE)
    product_code = 'DSO'
    check_if_type_supplied(product_code, flavor)
    unless config_source[MAVEN_REPO_CONFIG_KEY]
      @internal_config_source[MAVEN_REPO_CONFIG_KEY] = MAVEN_REPO_LOCAL
    end
    puts "Maven install #{flavor.upcase} artifacts to #{@internal_config_source[MAVEN_REPO_CONFIG_KEY]}"
    dist_jars(product_code, 'common', flavor)
    package_sources_artifacts(@config['package_sources']) if @config_source['include_sources'] == 'true' && @config['package_sources']
    deploy_maven_artifacts(@config['maven_deploy'])
  end

  private
  def call_actions(*actions)
    load_config
    actions.each { |action| method(action.to_sym).call }
  end

  require 'extensions/distribution-utils'
  include DistributionUtils

  require 'extensions/bundled-components'
  include BundledComponents

  require 'extensions/bundled-vendors'
  include BundledVendors

  require 'extensions/bundled-demos'
  include BundledDemos

  require 'extensions/bundled-modules'
  include BundledModules

  require 'extensions/packaging'
  include Packaging

  require 'extensions/postscripts'
  include Postscripts

  def __publish(archive_dir=nil)

    unless config_source["release-dir"].nil?
      release_dir = FilePath.new(config_source["release-dir"]).ensure_directory
    end

    destdir = release_dir || archive_dir              ||
      FilePath.new(config_source['build-archive-dir'] ||
        ".", @build_environment.current_branch, "rev#{@build_environment.os_revision}", @flavor).ensure_directory

    incomplete_tag = "__incomplete__"
    Dir.glob("#{@distribution_results.build_dir.to_s}/*").each do | entry |
      next if File.directory?(entry)
      filename            = File.basename(entry)
      incomplete_filename = destdir.to_s + "/" + filename + incomplete_tag
      dest_filename       = destdir.to_s + "/" + filename
      ant.copy(:file => entry, :tofile => incomplete_filename)
      FileUtils.rm(dest_filename) if File.exist?(dest_filename)
      ant.move(:file => incomplete_filename, :tofile => dest_filename)
    end
  end

  def __assemble
    exec_section :bundled_components
    exec_section :bundled_modules
    if @no_extra || @no_demo
      loud_message("--no-demo option found. No demos will be assembled")
    else
      exec_section :bundled_vendors
      exec_section :bundled_demos
    end
    puts "EXEC POSTSCRIPTS"
    exec_section :postscripts

    if (timlist = @config_source['timlist']) && (forgedir = @config_source['forgedir'])
      timlist = File.expand_path(timlist)
      forgedir = File.expand_path(forgedir)
      destdir = "#{product_directory.to_s}/platform/modules".gsub(/\\/, "/")

      cmd = "buildscripts/timbuild.sh #{timlist} #{forgedir} #{destdir}"

      puts "EXEC #{cmd}"
      result = system("bash #{cmd}")

      fail("timbuild.sh script failed") unless result
    end
  end

  def __package
    exec_section :packaging
    product_directory.delete
  end

  def package_sources_artifacts(args)
    return unless args
    args.each do |arg|
      destdir = FilePath.new(arg['dest']).ensure_directory
      puts "packaging #{arg['artifact']} to #{destdir.to_s}"
      ant.jar(:jarfile => "#{destdir.to_s}/#{arg['artifact']}.jar") do
        ant.fileset(:dir => @basedir.to_s, :includes => arg['includes'], :excludes => arg['excludes'])
      end
    end
  end

  def deploy_maven_artifacts(args)
    patch_descriptor = get_patch_descriptor
    if @build_environment.patch_branch?
      loud_message("This is a patch branch. Patch level will be added to Maven artifacts as part of their version")
    end
    
    if repo = @config_source[MAVEN_REPO_CONFIG_KEY]
      maven = MavenDeploy.new(:repository_url => repo,
        :repository_id => @config_source[MAVEN_REPO_ID_CONFIG_KEY],
        :snapshot => @config_source[MAVEN_SNAPSHOT_CONFIG_KEY])

      # rudimentary check to make sure we're not missing an artifact by mistake
      expected_count = args.shift['artifact_count']
      fail("Expecting to deploy #{expected_count} TC maven artifacts but found only #{args.size}") unless args.size == expected_count

      args.each do |arg|
        next if arg['classifier'] =~ /sources/ && @config_source['include_sources'] != 'true'
        if arg['file']
          file = FilePath.new(@basedir, interpolate(arg['file']))
        else
          file = FilePath.new(arg['srcfile'])
        end

        %w(inject inject_optional).each do |step|
          if arg[step]
            # Copy jar to tmp jar in same dir
            replacement_file = FilePath.new(file.directoryname) << file.filename + '.tmp'
            @ant.copy(:tofile => replacement_file.to_s, :file => file.to_s)
            file = replacement_file

            arg[step].each do |inject|
              # Inject resource into jar
              inject_file = FilePath.new(@basedir, interpolate(inject))
              next if step == 'inject_optional' && !inject_file.exist?
              @ant.create_jar(replacement_file,
                :update => 'true',
                :basedir => inject_file.directoryname,
                :includes => inject_file.filename)
            end
          end
        end
        
        group = arg['groupId']
        artifact = arg['artifact']
        classifier = arg['classifier']
        version = arg[MAVEN_VERSION_CONFIG_KEY] || @config_source[MAVEN_VERSION_CONFIG_KEY] ||
          @config_source['version'] || @build_environment.version
        
        if (@build_environment.patch_branch?)
          version = version + "-patch#{patch_descriptor['level']}"
        end

        # Allow override of version if a version key is specified.  If so, the value of the key
        # is the property to look up as defined in build-config.global, etc.
        if arg['version']
          versionKey = arg['version']
          version = arg[versionKey] || @config_source[versionKey]
        end

        maven.deploy_file(file.to_s, group, artifact, classifier, version, arg['pom'])

        # clean up injected file if it existed
        if arg['inject']
          file.delete
        end
      end
    end
  end
  
  def get_patch_descriptor
    descriptor_file = self.patch_descriptor_file.to_s
    unless File.readable?(descriptor_file)
      raise("Patch descriptor file does not exist: #{descriptor_file}")
    end

    patch_descriptor = YAML.load_file(descriptor_file)
    unless patch_descriptor['level'] && patch_descriptor['files'].is_a?(Array)
      raise("Invalid patch descriptor file")
    end
    patch_descriptor
  end
end
