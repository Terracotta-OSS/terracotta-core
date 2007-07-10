#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # assemble the kit for the product code supplied
  def dist(product_code='DSO', flavor='OPENSOURCE')
    check_if_type_supplied(product_code, flavor)
    
    depends :init, :compile
    call_actions :__assemble
  end
  
  def dist_all(flavor='OPENSOURCE')
    @flavor = flavor.downcase    
    depends :init, :compile
    srcdir        = @static_resources.distribution_config_directory(flavor).canonicalize.to_s
    product_codes = Dir.entries(srcdir).delete_if { |entry| (/\-(#{flavor})\.def\.yml$/i !~ entry) || (/^x\-/i =~ entry) }
    product_codes.each do |product_code| 
      @product_code = product_code.sub(/\-.*$/, '')
      @flavor       = flavor
      call_actions :__assemble
      srcdir  = product_directory.to_s
      destdir = FilePath.new(@distribution_results.archive_dir.ensure_directory, package_filename).to_s
      ant.move(:file => srcdir, :todir => destdir)
    end
  end
  
  # assemble and package the kits  for the product code supplied
  def create_package(product_code='DSO', flavor='OPENSOURCE')
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile
    call_actions :__assemble, :__package
  end
  
  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory)
  def create_all_packages(flavor='OPENSOURCE')
    @flavor = flavor.downcase
    
    depends :init, :compile
    srcdir        = @static_resources.distribution_config_directory(flavor).canonicalize.to_s
    product_codes = Dir.entries(srcdir).delete_if { |entry| (/\-(#{flavor})\.def\.yml$/i !~ entry) || (/^x\-/i =~ entry) }
    product_codes.each do |product_code| 
      @product_code = product_code.sub(/\-.*$/, '')
      @flavor       = flavor
      call_actions :__assemble, :__package
      __publish @distribution_results.archive_dir.ensure_directory
    end
  end

  # assemble, package, and publish the kit for the product code supplied
  def publish_package(product_code='DSO', flavor='OPENSOURCE')
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile
    call_actions :__assemble, :__package, :__publish
  end
  
  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory)
  def publish_all_packages(flavor='OPENSOURCE')
    @flavor = flavor.downcase
    
    depends :init, :compile
    srcdir        = @static_resources.distribution_config_directory(flavor.downcase!).canonicalize.to_s
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
    publish_all_packages('OPENSOURCE')
  end
  
  # HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK HACK
  # assemble, package, and publish all possible kits (based on the configuration
  # files found under buildconfig/distribution directory) for the enterprise version
  # of the product
  def publish_enterprise_packages
    publish_all_packages('ENTERPRISE')
  end
  
  # build the JAR files for the product code supplied without assembling a full kit
  def dist_jars(product_code='DSO', component_name='common', flavor='OPENSOURCE')
    check_if_type_supplied(product_code, flavor)

    depends :init, :compile, :load_config
    component = get_spec(:bundled_components, []).find { |component| /^#{component_name}$/i =~ component[:name] }
    libdir    = FilePath.new(@distribution_results.build_dir, 'lib.tmp').ensure_directory
    destdir   = FilePath.new(@distribution_results.build_dir, 'lib').ensure_directory
    add_binaries(component, libdir, destdir)
    libdir.delete
    
    add_module_packages(component, destdir)
    add_dso_bootjar(component, destdir, true)
    ant.move(:todir => FilePath.new(File.dirname(destdir.to_s), 'tc-jars').to_s) do
      ant.fileset(:dir => destdir.to_s, :includes => '**/*')
    end
    destdir.delete
  end
  
  private
  def call_actions(*actions)
    load_config
    @distribution_results.clean(ant)
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

  require 'extensions/bundled-jres'
  include BundledJREs

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
      ".", @build_environment.current_branch, "rev#{@build_environment.current_revision}", @build_environment.os_family.downcase).ensure_directory
      
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
    exec_section :bundled_vendors
    exec_section :bundled_demos
    exec_section :bundled_jres
    exec_section :bundled_modules
    exec_section :postscripts
  end

  def __package
    exec_section :packaging
    product_directory.delete
  end
end
