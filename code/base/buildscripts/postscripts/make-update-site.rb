#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - create the eclipse update site
  protected
  def postscript(ant, build_environment, product_dir, *args)
    prod_version   = get_config(:version, "1.0.0")
    plugin_version = prod_version+".0" if prod_version.length == 3

    eclipse_dir = FilePath.new(product_dir, 'eclipse')
    plugin_name = "org.terracotta.dso_" + plugin_version
    prod_name   = "org.terracotta.dso_" + prod_version
    plugin_dir  = FilePath.new(eclipse_dir, prod_name)
    jar_name    = plugin_name + ".jar"

    update_dir   = FilePath.new(eclipse_dir, 'update')
    features_dir = FilePath.new(update_dir, 'features')
    plugins_dir  = FilePath.new(update_dir, 'plugins')

    feature_file = FilePath.new(features_dir, 'feature.xml')
    feature_jar  = FilePath.new(features_dir, jar_name)

    ant.replace(:file => feature_file, :token => "VERSION", :value => plugin_version)
    ant.jar(:destfile => feature_jar.to_s, :basedir => features_dir.to_s)
    ant.delete(:file => feature_file.to_s)

    plugin_archive = FilePath.new(plugins_dir, jar_name)
    
    ant.mkdir(:dir => plugins_dir.to_s)
    ant.zip(:destfile => plugin_archive.to_s, :basedir => plugin_dir.to_s)
    ant.delete(:dir => plugin_dir.to_s)

    site_file = FilePath.new(update_dir, 'site.xml')
    ant.replace(:file => site_file, :token => "VERSION", :value => plugin_version)
  end
end
