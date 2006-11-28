require 'tmpdir'

module BundledVendors
    def bundled_vendors(name, directory, spec)
        #~ srcdir = FilePath.new(@static_resources.vendors_directory, name)
        srcdir = getCachedVendorDir(name)
        fail "The source for the named vendor set: `#{name}' does not exists in #{@static_resources.vendors_directory}" unless File.directory?(srcdir.to_s)

        wildcard = '/**/*'
        manifest = (spec[:manifest] || []).join("#{wildcard}, ") 
        puts :warn, "The vendor set: `#{name}' has an empty manifest." if manifest.empty?

        unless manifest.empty?
            # copy vendor sources and binaries but exclude non-native scripts
            non_native = @build_environment.is_unix_like? ? ['*.bat', '*.cmd', '*.exe'] : '*.sh'
            destdir    = FilePath.new(product_directory, directory).ensure_directory
            ant.copy(:todir => destdir.to_s) do
                ant.fileset(:dir => srcdir.to_s, :excludes => "**/.svn/**, **/.*, **/**/#{non_native.to_a.join(', **/**/')}", :includes => manifest + wildcard)
            end

            # check that every vendor listed in the manifest was copied and make sure that it can be rebuilt
            vendors = Dir.entries(destdir.to_s)
            manifest.split("#{wildcard}, ").each do |entry| 
                fail "The vendor set `#{name}/#{entry}' was not copied. Please check to make sure that the sources for this vendor exist.'" unless vendors.include? entry 
            end
        end
    end

    def getCachedVendorDir(vendor)
        url = @static_resources.vendors_url + "/" + vendor + ".zip"
        zipfile = FilePath.new(Dir.tmpdir, vendor + ".zip").to_s
        
        ant.get(:src => url, :dest => zipfile, :usetimestamp => true)
        ant.unzip(:src => zipfile, :dest => Dir.tmpdir, :overwrite => true)
        FilePath.new(Dir.tmpdir, vendor).to_s
    end
end
