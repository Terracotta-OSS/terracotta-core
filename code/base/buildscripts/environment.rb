#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# A class that lets you determine, in a consistent, cross-platform manner, a number of
# properties about the environment in which your Ruby code is running. This is things
# like the hostname, the operating-system type, the username, and so on.
class Environment
    # Creates a new Environment object. platform is the correct Platform instance
    # for your platform -- see cross_platform.rb for details.
    def initialize(platform)
        @platform = platform
        @os_types = { }
    end

    # What's the name of the host this code is running on?
    def hostname
        if @hostname.nil?
            @hostname = @platform.get_env("COMPUTERNAME")
            @hostname = @platform.exec("hostname") if @hostname.blank?
            @hostname = @hostname.gsub(/\.terracotta\.lan/, '').strip
        end
        
        @hostname
    end

    # Are we running on a lame-ass broken OS that has problems creating files that have
    # a full pathname longer than a short length (like, say, Windows and 255-character-or-more
    # pathnames)?
    def has_pathname_length_limitations?
        # Goddamn, does Windows ever suck. Man.
        os_type =~ /^\s*windows\s*$/i
    end

    # What type of operating system are we running on? If nice is :nice, use 'Windows' instead
    # of 'CYGWIN', 'OSX' instead of 'Darwin', and 'Solaris' instead of 'SunOS'. If extended
    # is :extended, then further classify Linux into distribution type.
    def os_type(nice=:not_nice, extended=:not_extended)
        # We cache the OS type, because finding it involves running programs -- we ask for this
        # often enough in some cases that this actually becomes a very significant bottleneck
        # if we don't cache it.
        key = [ nice, extended ]
        
        if @os_types[key].nil?
            if @platform.get_env("OS") =~ /windows/i
                os_type = "Windows"
            else
                os_type = @platform.exec("uname", "-s").strip if @os_type.nil?
                os_type = "Windows" if (nice == :nice) && (/CYGWIN/ =~ os_type)
                os_type = "OSX"     if (nice == :nice) && (/Darwin/ =~ os_type)
                os_type = "Solaris" if (nice == :nice) && (/SunOS/  =~ os_type)
            end

            os_type.chomp!

            if os_type =~ /linux/i && extended == :extended
                os_type = linux_type
            end
            
            @os_types[key] = os_type
        end

        @os_types[key]
    end

    # What's the name of the user running this Ruby code?
    def username
        if @username.nil?
            if os_type(:nice, :not_extended) == 'Solaris'
                @username = @platform.exec("/usr/ucb/whoami").strip # Fucking Solaris
            else
                @username = @platform.exec("whoami").strip
            end
        end

        @username
    end

    # What operating-system family are we on -- Unix, Windows, or other? (OS X counts as Unix here.)
    def os_family
        case os_type(:nice)
        when /Linux|Solaris|OSX/i then 'Unix'
        when /Windows/i           then 'Windows'
        else                           'Unknown'
        end
    end

    # What platform are we on? This combines operating system, patch level (like service
    # pack) -- or at least should, but doesn't in all cases, and processor architecture.
    # This should contain all information about the operating system that could reasonably
    # be expected to affect the results of tests.
    #
    # Returns strings that are things like 'rhel4-update-2-athlon' and 'win2k3-i686'.
    def platform
        type = os_type(:nice)
        type = case type
        when /Linux/i then linux_type
        else type
        end
        
        # Grab the Solaris version; turns '5.9' into '9', for example. (Solaris has a really
        # fucked-up, bizarre numbering scheme.)
        #
        # FIXME: This should include patchlevel.
        if type =~ /Solaris/i
            if @solaris_version.nil?
                @solaris_version = @platform.exec("uname", "-r").strip
                @solaris_version = $2 if @solaris_version =~ /^\s*(\d+)\s*\.\s*(\d+)\s*$/i
            end
            
            type += @solaris_version
        end
        
        # Grab the OS X version by looking at the Darwin kernel version.
        #
        # FIXME: This should include minor version, too (e.g., 10.4.7).
        if type =~ /OSX/i
            if @osx_version.nil?
                @osx_version = @platform.exec("uname", "-r").strip
                if @osx_version =~ /^\s*(\d+)\s*\.\s*(\d+).*$/i
                    minor_version = [ $1.to_i - 4, 0 ].max # i.e., Darwin 8.x => OS X 10.4, 7.x => OS X 10.3, etc.
                    @osx_version = "10.%d" % minor_version
                else
                    @osx_version = '-unknown-%s' % @osx_version
                end
            end
            
            type += @osx_version
        end
        
        # Grab the Windows version by looking at the Cygwin version.
        #
        # FIXME: This should include service pack, too (e.g., win2k3-sp2).
        if type =~ /Windows/i
            if @windows_version.nil?
                @windows_version = @platform.exec("uname", "-s").strip
                if @windows_version =~ /CYGWIN_NT-(\d+)\.(\d+)/i
                   @windows_version = case $2.to_i
                   when 1 then 'winxp'
                   when 2 then 'win2k3'
                   else 'windows-unknown-rev-%s' % @windows_version
                   end
                else
                   @windows_version = 'windows-unknown-%s' % @windows_version
                end
            end
            
            type = @windows_version
        end

        "%s-%s" % [ type.downcase, processor_type.downcase ]
    end

    # What processor architecture are we running on? This is something like 'i686', 'powerpc', or
    # 'athlon'.
    def processor_type
        if @processor_type.nil?
            @processor_type = @platform.exec("uname", (os_type(:nice) =~ /Windows/ ? '-m' : '-p')).strip
        end
        
        @processor_type
    end

    # Is this a Unix-like OS (right now, anything but Windows)?
    def is_unix_like?
        os_type !~ /^\s*windows\s*$/i
    end

    private
    # What type of Linux are we running on?
    def linux_type
        redhat_linux_type || suse_linux_type || unknown_linux_type
    end

    # If we're running on RedHat Linux, a string representing the exact version (something
    # like 'rheles4-update-2'). If not, nil.
    def redhat_linux_type
        if @redhat_linux_type.nil?
            out = nil
            text = FilePath.new("/etc/redhat-release").suck_file
            unless text.nil?
                if text =~ /red\s*hat/i
                    out = "rh"
                    out += "el" if text =~ /enterprise/i
                    out += "es" if text =~ /\s+ES\s+/i
                    out += "%s" % $1 if text =~ /release\s+(\d+)/i
                    out += "-update-%s" % $1 if text =~ /update\s+(\d+)/i
                end
            end
            
            @redhat_linux_type = out
        end

        @redhat_linux_type
    end

    # If we're running on SuSE Linux, a string representing the exact version
    # (something like 'sles10'). If not, nil.
    def suse_linux_type
        if @suse_linux_type.nil?
            out = nil
            text = FilePath.new("/etc/SuSE-release").suck_file
            unless text.nil?
                if text =~ /suse/i
                    out = "sl"
                    out += "es" if text =~ /enterprise\s*server/i
                    out += "%s" % $1 if text =~ /^[^\d]+(\d+)/i
                end
            end
            
            @suse_linux_type = out
        end

        @suse_linux_type
    end

    # On any Linux, a string representing the Linux kernel version.
    def unknown_linux_type
        if @unknown_linux_type.nil?
            version = @platform.exec("uname", "-r").chomp
            version = "unknown" if version.blank?
            @unknown_linux_type = "unknown-linux-%s" % version
        end
        
        @unknown_linux_type
    end
end
