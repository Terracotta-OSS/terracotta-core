#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module Packaging
  def packaging(name, directory, spec)
    destdir = FilePath.new(@distribution_results.build_dir, directory) 
    srcdir  = product_directory
    load FilePath.new(@static_resources.extensions_directory, "#{name}-package.rb").to_s
    destfile = method(:make_package).call(srcdir, destdir, package_filename, root_directory, get_config(:internal_name))
    ant.copy(:file => destfile.to_s, :todir => package_directory.to_s)
  end
end