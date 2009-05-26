#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#
require 'net/http'
require 'uri'

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  # - create the eclipse update site
  protected
  def postscript(ant, build_environment, product_dir, *args)
    
    plugin_version = create_eclipse_plugin_version(build_environment)

    eclipse_dir = FilePath.new(product_dir, 'eclipse')
    plugin_name = "org.terracotta.dso_" + plugin_version    
    plugin_dir  = FilePath.new(eclipse_dir, plugin_name)
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
    retrieveSiteXml(site_file)
    ant.replace(:file => site_file, :token => "VERSION", :value => plugin_version)
  end
  
  def retrieveSiteXml(site_file)
    url = "http://svn.terracotta.org/svn/tc/eclipse-update-site/site.xml"
    begin
      res = Net::HTTP.get_response(URI.parse(url))
      res.value
      puts "Records of published TC plugins read from #{url}"
      File.open(site_file.to_s, "w") do |f|
        f.puts res.body
      end
    rescue
      STDERR.puts "Can't read records of published TC plugins from #{url}"
    end
  end
end
