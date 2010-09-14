require 'fileutils'
require 'uri'

# Utility class for downloading remote resources, optionally caching them for
# future reuse.
# This class makes use of the global Registry object, so it must be used in a
# context where the Registry has already been initialized.
class DownloadUtil

  # Creates a new download utility that uses the given cache_dir to cache
  # downloaded resources (if given).
  def initialize(cache_dir = nil)
    if cache_dir
      @cache_dir = cache_dir.to_s
      FileUtils.mkdir_p(@cache_dir)
    end
  end

  def get(url, dest_file)
    download_path = cache_path(url, dest_file)

    ant.get(:src => url, :dest => download_path, :usetimestamp => true,
            :verbose => true)
    if download_path != dest_file
      FileUtils.copy(download_path, dest_file)
    end
  end

  private

  def ant
    Registry[:ant]
  end

  def cache_path(url, dest_file)
    if @cache_dir
      uri = URI.parse(url)
      cache_dir = File.join(@cache_dir, File.join(uri.host, File.dirname(uri.path)))
      FileUtils.mkdir_p(cache_dir)
      File.join(cache_dir, File.basename(uri.path))
    else
      dest_file
    end
  end
end
