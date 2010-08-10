<?php
class TemplateEngine
{
    protected $template_dir;
    protected $template_content;
    protected $var_format;
    protected $var_open;
    protected $var_close;
    protected $vals;
    protected $available_funcs;
    protected $default_vals;
    protected $var_max_len;
    protected $func_max_len;
    public function __construct($template_dir)
    {
        if(!file_exists($template_dir)) throw new Exception("{$template_dir} doesn't exist!");
        $this->template_dir = $template_dir;
        $this->var_format = $this->setVarFormat('{%s}');
        $this->available_funcs =  array('iif', 'loop', 'qs');
        $this->default_vals = array('_self' => $_SERVER['PHP_SELF'], '_query_string' => $_SERVER['QUERY_STRING']);
        $this->func_max_len = array_reduce(array_map('strlen', $this->available_funcs), 'max', 0);
    }
    public function register($template)
    {
        if(!file_exists($this->template_dir.$template)) return false;
        $this->template_content .= file_get_contents($this->template_dir.$template);
        return true;
    }
    /**
     * vals can be an array or a string
     * if vals->is_array it gets merged with the class vals
     * else vals is used as key and val as value and get appended to class vals
     */
    public function assign($vals, $val = '')
    {
        if(is_array($vals)) $this->vals = array_merge($this->vals, $vals);
        else $this->vals[$vals] = $val;
    }
    public function printOut($ret = false)
    {
        if(empty($this->template_content)) return false;
        
        $this->parseIncludes();
        $this->parseVars();
        $this->parseFuncs();
        
        if($ret) return $this->template_content;
        echo $this->template_content;
        return true;
    }
    public function parseVars()
    {
        $this->vals = array_merge($this->vals, $this->default_vals);
        foreach($this->vals as $k => $v)
        {
            $this->template_content .= str_replace($this->var_open.$k.$this->var_close, $v, $this->template_content);
        }
    }
    public function includeAt($var, $template)
    {
        if(!file_exists($this->template_dir.$template)) return false;
        $temp = file_get_contents($this->template_dir.$template);
        $this->template_content = str_replace($this->var_open.$var.$this->var_close, $temp, $this->template_content);
        return true;
    }
    public function parseFuncs()
    {
        #find starting point
        $pos = strpos($this->template_content, '[');
        
        while($pos !== false)
        {
            #find opening brace
            $brace_pos = strpos($this->template_content, '(', $pos);
            #func is everything between [ and (
            $func = trim(substr($this->template_content, $pos+1, $brace_pos-$pos-1));
            if($brace_pos !== false && strlen($func) <= $this->func_max_len)
            {
                if(!in_array($func, $this->available_funcs)) continue;

                #now find closing brace
                $close_brace_pos = strpos($this->template_content, '}', $brace_pos);
                $close_bracket_pos = strpos($this->template_content, ']', $close_brace_pos);
                
                #compose 'real' method name
                $call_func = 'UDF_'.$func;
                $params = explode(',', substr($this->template_content, $brace_pos+1, $close_brace_pos-$brace_pos-1));
                
                $replace = call_user_func_array($call_func, $params);
                
                $this->template_content = substr_replace($this->template_content, $replace, $pos, ($close_bracket_pos+1)-$pos);
                $pos += strlen($replace);
            }
            else
            {
                #if brace_pos == false, we can safely assume there are no remaining functions, so put pointer at end of stream
                if($brace_pos === false)
                {
                    $pos = strlen($this->template_content);
                }
                else #func name too long
                {
                    #find a [ between pos and brace_pos
                    $tmp_pos = strripos(substr($this->template_content, 0, $brace_pos+1), '[', $pos+1);
                    if($tmp_pos === false)
                    {
                        $pos = $brace_pos;
                    }
                    else
                    {
                        $pos = $tmp_pos - 1;
                    }
                }
            }
            $pos = strpos($this->template_content, '[', $pos-1);
        }
    }
    private function UDF_iif($cond, $iftrue, $iffalse)
    {
        return ($cond ? $iftrue : $iffalse);
    }
    private function UDF_loop($array_var, $loop_string)
    {
        $array_vars = trim(str_replace('%', '', $array_var));
        
        $ret_var = '';
        
        if(!isset($this->vals[$array_var]) || !is_array($this->vals[$array_var])) return false;
        
        foreach($this->vals[$array_var] as $k => $v)
        {
            if(!is_array($v)) return false;
            
            $tmp = $loop_string;
            foreach($v as $k2 => $v2)
            {
                $tmp = str_replace("%{$array_var}[$k2]%", $v2, $tmp);
                
                $ret_var .= $tmp;
            }
        }
        
        return $ret_var;
    }
    private function UDF_qs($var, $val)
    {
        $qs = $_SERVER['QUERY_STRING'];
        $qsa = array();
        parse_str($qs, $qsa);
        $qsa[$var] = $val;
        return '?'.http_build_query($qsa);
    }
    private function parseIncludes()
    {
        $pattern = "/\[\[(.*)\]\]/";
        preg_match_all($pattern, $this->template_content, $matches);
        if(empty($matches)) return false;
        for($x=0;$x<count($matches[0]);++$x)
        {
            if(!file_exists($this->template_dir.$matches[1][$x]))
            {
                $to_replace = '';
            }
            else
            {
                $to_replace = file_get_contents($this->template_dir.$matches[1][$x]);
            }
            $this->template_content = str_replace($matches[0][$x], $to_replace, $this->template_content);
        }
        #recurse!
        $this->parseIncludes();
    }
    
    public function setVarFormat($format)
    {
        list($this->var_open, $this->var_close) = explode('%s', $format, 2);
    }
    public function setTemplateContent($content)
    {
        $this->template_content = $content;
    }
}