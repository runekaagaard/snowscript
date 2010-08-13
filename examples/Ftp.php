<?php
class FTP {
    const OPT_TRANS_ASCII = FTP_ASCII;
    const OPT_TRANS_BINARY = FTP_BINARY;
    const OPT_TRANS_AUTO = 3;

    protected $ascii_types = array("text", "csv");
    protected $binary_types = array("jpg", "jpeg", "gif", "psd");
    protected $default_transmode = self::OPT_TRANS_ASCII;

    #connection resource
    protected $conn;

    #constructs FTP object
        #Wrapper on top of FTP protocol
        #Enables file/directory listing, upload/download etc
    #@classvar protected str host
        #the ftp host to connect to
        #!%->empty
    #@classvar protected int port=21
        #the port to connect (defaults to 21)
        #%>0 && %<65536
    #@classvar protected int timeout=30
        #connect timeout, bail out if exceeded
        #defaults to 30, 0 means wait forever
    public function __construct($host, $port=21, $timeout=30) {
        $this->host = $host;
        $this->port = $port;
        $this->timeout = $timeout;
        $this->conn = ftp_connect($host, $port, $timeout);
        if (empty($this->conn)) {
            throw Exception("Couldn't connect to host '%host'");
        }

    }
    #@str user
    #@str pass
    function login($user, $pass) {
        return ftp_login($this->conn, $user, $pass);
    }

    #@str local
    #@str remote
    #@int trans_mode
    function put($local, $remote, $trans_mode) {
        if ($trans_mode == self:: OPT_TRANS_AUTO) {
            $trans_mode = getTransMode($local);
        }
        if (FALSE === ftp_put($this->conn, $local, $remote, $trans_mode)) {
            throw Exception("Couldn't get file '%remote'");
        }
    }

    #@str local
    #@str remote
    #@int trans_mode
    function get($local, $remote, $trans_mode) {
        if ($trans_mode == self:: OPT_TRANS_AUTO) {
            $trans_mode = getTransMode($local);
        }
        if (FALSE === ftp_get($this->conn, $local, $remote, $trans_mode)) {
            throw Exception("Couldn't get file '%remote'");
        }
    }
    #checks whether a file exists on the remote side
    #@str file
        #the filename to check for
        #!%->empty
    function fileExists($file) {
        $list = ftp_nlist($this->conn, $file->dirname);
        return in_array($list, $file->basename);
    }
    
    #determine transfermode to use
    #@str file
    function getTransMode($file) {
        foreach ($this->ascii_types as $ext) {
            if (substr($file, $ext->strlen) == $ext) {
                return self::OPT_TRANS_ASCII;
            }
        }
        foreach ($this->ascii_types as $ext) {
            if (substr($file, $ext->strlen) == $ext) {
                return self::OPT_TRANS_BINARY;
            }
        }
        return $this->default_transmode;
    }

    function close() {
        return ftp_close($this->conn);
    }

}

#try
#    ftp = FTP("my.host")
#    ftp.login("user" "pass")
#    ftp.get("/remote/file" "/local/file")
#catch: e->var_dump
