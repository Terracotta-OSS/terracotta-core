
class SvnInfo
  def initialize(dir)
    @svninfo = {}
    @svninfo["Last Changed Rev"] = 0
    @svninfo["Last Changed Author"] = "unknown"
    @svninfo["Last Changed Date"] = "unknown"
    @svninfo["URL"] = "unknown"  
    if dir
      dir = dir.gsub("\\", "/").to_shell_escaped_s
      svn_info = `svn info "#{dir}" 2>&1`
      if $? == 0 && !svn_info.blank?
        begin
          @svninfo.merge!(YAML::load(svn_info))
          @valid = true
        rescue
          STDERR.puts("Error retrieving SVN info: #{svn_info}")
        end
      end   
    end
  end
  
  def valid?
    @valid
  end
  
  def last_changed_rev
    @svninfo["Last Changed Rev"]
  end
  
  def last_changed_author
    @svninfo["Last Changed Author"]
  end
  
  def last_changed_date
    @svninfo["Last Changed Date"]
  end
  
  def url
    @svninfo["URL"]
  end
  
  def revision
    last_changed_rev
  end
end
