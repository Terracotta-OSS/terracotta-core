#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

require 'tmpdir'
require 'socket'

module BundledVendors
  def bundled_vendors(name, directory, spec)
    url       = "#{@static_resources.vendors_url}/#{name}.zip"
    unless is_online?
      puts "*" * 30 + " WARNING " + "*" * 30
      puts "Unable to retrieve #{name} bundle... skipping"
      puts "*" * 30 + " WARNING " + "*" * 30
      return
    end
      
    srcdir = getCachedVendorDir(url, name)
    fail "The source for the named vendor set: `#{name}' does not exists in #{srcdir}" unless File.directory?(srcdir)
    # copy vendor sources and binaries but exclude non-native scripts
    destdir    = FilePath.new(product_directory, directory).ensure_directory
    ant.copy(:todir => destdir.to_s) do
      ant.fileset(:dir => srcdir.to_s, :excludes => "**/.svn/**, **/.*")
    end
  end

  def getCachedVendorDir(url, vendor)
    cache_dir = FilePath.new(ENV['HOME'], '.tc').ensure_directory.canonicalize.to_s
    zipfile   = FilePath.new(cache_dir, vendor + ".zip").to_s
    ant.get(:src => url, :dest => zipfile, :usetimestamp => true)
    ant.unzip(:src => zipfile, :dest => cache_dir, :overwrite => true)
    FilePath.new(cache_dir, vendor).to_s
  end
      
  def is_online?
    begin
      socket = TCPSocket.new("download.terracotta.org", 80)
      socket.close
      return true
    rescue
      return false
    end    
  end
  
end
