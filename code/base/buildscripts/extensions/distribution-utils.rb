#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module DistributionUtils

  def exec_section(name)
    get_spec(name, []).each do |section|
      (section[:install_directories] || [(section[:install_directory] || '')]).each do |directory|
        send(name, section[:name], directory, section)
      end
    end
  end
  
  def load_config
    product_code = (@product_code || @config_source["product.code"]).downcase
    flavor       = (@flavor || @config_source["flavor"]).downcase
    filename     = FilePath.new(@static_resources.distribution_config_directory(flavor), "#{product_code}-#{flavor}.def.yml").canonicalize.to_s
    File.open(filename) { |file| @config = YAML.load(file) } if File.exist?(filename)
    fail "You need to create a kit definition file named `#{filename}' before you can build distribution for a `#{product_code}' kit." if @config.nil?
    
    @distribution_results = DistributionResults.new(FilePath.new(@build_results.build_dir, "dist"))
  end

  def check_if_type_supplied(product_code, flavor)
    fail 'You need to tell me the type of kit to build: DSO?'                         if product_code.nil? 
    fail 'You need to tell me the flavor of the kit to build: OPENSOURCE|ENTERPRISE?' if flavor.nil? 
    @product_code = product_code
    @flavor       = flavor.downcase
  end
  
  def product_directory
    FilePath.new(@distribution_results.build_dir, get_config(:package_directory)).ensure_directory
  end

  def dorevpath(component)
    suffix = 'dorev' unless component[:install_directory].nil?
    FilePath.new(product_directory, (component[:install_directory] || ''), (suffix || ''))
  end

  def docspath(component)
    suffix = 'docs' unless component[:install_directory].nil?
    FilePath.new(product_directory, (component[:install_directory] || ''), (suffix || ''))
  end

  def libpath(component)
    FilePath.new(product_directory, (component[:install_directory] || ''), 'lib')
  end

  def native_libpath(component)
    FilePath.new(libpath(component), 'native')
  end

  def package_filename
    "#{get_config(:short_internal_name).downcase}-" +
    "#{@build_environment.os_family.downcase}-" +
    "#{@build_environment.processor_type}-" +    
    "#{@build_environment.build_hostname.downcase}-rev" +
    "#{@build_environment.current_revision}" 
  end

  def get_spec(symbol, default=nil)
    out     = []
    configs = @config[symbol.to_s]
    configs.each do |item|
      if item.instance_of?(Hash)
        spec = { :name => item.keys[0] }
        spec.merge!(item.values[0])
        spec = symbolise_keys(spec) if spec.instance_of?(Hash)
        out << spec if spec[:assert].nil? || eval(spec[:assert]) 
      else
        out << { :name => item }
      end
    end unless configs.nil?
    out
  end

  def get_config(symbol, default=nil)
    case symbol
    when :version           then @config[symbol.to_s] || @build_environment.specified_build_version
    when :package_directory then @config[symbol.to_s] || "#{get_config(:root_directory)}-#{get_config(:version)}"
    else
      out = @config[symbol.to_s] || default
      #symbolise_keys(out) if out.instance_of?(Hash)
      out
    end 
  end

  def symbolise_keys(hash)
    hash.each_key { |k| hash[k.to_sym] = hash.delete(k) unless k.instance_of?(Symbol) }
    hash
  end

 def docspath(component, install_directory=nil)
   suffix = install_directory unless install_directory.nil?
   if suffix.nil?
     suffix = 'docs' unless component[:install_directory].nil?
   end
   FilePath.new(product_directory, (component[:install_directory] || ''), (suffix || ''))
 end 
end
