#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module BundledJREs
  def bundled_jres(name, directory, spec)
    version        = spec[:version]
    os_type        = @build_environment.os_type(:nice).downcase
    processor_type = @build_environment.processor_type.downcase
    jre_filename   = "jre-with-jdk-#{version}-#{os_type}.tar.gz"
    jre_url        = "#{@static_resources.jre_url}/#{os_type}/#{jre_filename}" 
    jre_srcdir     = FilePath.new(ENV['HOME'], '.tc').ensure_directory
    jre_srcfile    = FilePath.new(jre_srcdir, jre_filename)
    begin
      unless File.exist?(jre_srcfile.to_s)        
        ant.get(:src => jre_url, :dest => jre_srcfile.to_s)
      end
      destdir = FilePath.new(product_directory, directory).ensure_directory
      ant.untar(:src => jre_srcfile, :dest => destdir.to_s, :compression => 'gzip', :overwrite => true) 
    rescue      
      puts "Unable to retrieve JRE bundle: `#{jre_filename}'"
      puts "Caught exception: #{$!}"
    end 
  end
end
