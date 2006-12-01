require 'tmpdir'

module BundledVendors
    def bundled_vendors(name, directory, spec)
        srcdir = getCachedVendorDir(name)
        fail "The source for the named vendor set: `#{name}' does not exists in #{srcdir}" unless File.directory?(srcdir)

        # copy vendor sources and binaries but exclude non-native scripts
        non_native = @build_environment.is_unix_like? ? ['*.bat', '*.cmd', '*.exe'] : '*.sh'
        destdir    = FilePath.new(product_directory, directory).ensure_directory
        ant.copy(:todir => destdir.to_s) do
          ant.fileset(:dir => srcdir.to_s, :excludes => "**/.svn/**, **/.*, **/**/#{non_native.to_a.join(', **/**/')}")
        end
    end

    def getCachedVendorDir(vendor)
        url       = "#{@static_resources.vendors_ur}/#{vendor}.zip"
        cache_dir = FilePath.new('.tc-build-cache').ensure_directory.canonicalize.to_s
        zipfile   = FilePath.new(cache_dir, vendor + ".zip").to_s
        ant.get(:src => url, :dest => zipfile, :usetimestamp => true)
        ant.unzip(:src => zipfile, :dest => cache_dir, :overwrite => true)
        FilePath.new(cache_dir, vendor).to_s
    end
end
