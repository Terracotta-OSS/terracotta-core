#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module Packaging
  def packaging(name, directory, spec)
    destdir = FilePath.new(@distribution_results.build_dir, directory) 
    srcdir  = product_directory
    require FilePath.new(@static_resources.extensions_directory, "#{name}-package.rb").to_s
    method(:make_package).call(srcdir, destdir, package_filename, get_config(:package_directory), get_config(:internal_name))
  end
end