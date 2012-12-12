<?php

function get_snow_tokens()
{
    $i = 1001;
    $snow_token_names = array('T_IN', 'T_TO', 'T_DOWNTO', 'T_STEP', 'T_THEN', 'T_STRING_SINGLE', 'T_STRING_DOUBLE', 'T_DOUBLE_QUESTION_MARK');
    $snow_tokens = array();
    foreach ($snow_token_names as $token_name) {
        define($token_name, $i);
        $snow_tokens[$i] = $token_name;
        $i += 1;
    }
    unset($token_name);
    return $snow_tokens;
}
global $snow_tokens;
$snow_tokens = get_snow_tokens();
function snow_token_name($i)
{
    global $snow_tokens;
    return isset($snow_tokens[$i]) ? $snow_tokens[$i] : token_name($i);
}
function get_named_tokenmap_cb($x)
{
    return str_replace("PHPParser_Parser::", "", $x);
}
function get_named_tokenmap()
{
    $named_tokenmap = array();
    for ($i = 256; $i <= 1100; ++$i) {
        if ($i === T_DOUBLE_COLON) {
            $named_tokenmap[$i] = 'T_PAAMAYIM_NEKUDOTAYIM';
        } elseif ($i === T_OPEN_TAG_WITH_ECHO) {
            $named_tokenmap[$i] = PHPParser_Parser::T_ECHO;
        } elseif ($i === T_CLOSE_TAG) {
            $named_tokenmap[$i] = ord(';');
        } else {
            $name = snow_token_name($i);
            if ($name !== 'UNKNOWN') {
                $const_name = "PHPParser_Parser::" . $name;
                if (defined($const_name)) {
                    $named_tokenmap[$i] = $name;
                }
            }
        }
    }
    unset($i);
    return array_flip(array_map('get_named_tokenmap_cb', $named_tokenmap));
}
class Snowscript_Lexer extends PHPParser_Lexer
{
    
    public function alter_token_type($t)
    {
        $type = "T_" . $t['type'];
        if (isset($this->token_types_map[$type])) {
            $type = $this->token_types_map[$type];
        }
        return $type;
    }
    
    public function alter_token_value($t, $altered_type)
    {
        $value = $t['value'];
        if (isset($this->transform_token_value[$altered_type])) {
            $value = sprintf($this->transform_token_value[$altered_type], $value);
        }
        return $value;
    }
    
    public function translate_token($t)
    {
        $type = $this->alter_token_type($t);
        $value = $this->alter_token_value($t, $type);
        if (isset($this->named_tokenmap[$type])) {
            $token_number = $this->named_tokenmap[$type];
            $result = is_array($value) ? $value[1] : $value;
            return array(array($token_number, $result, 2));
        } elseif (isset($this->literal_tokens[$type])) {
            return $value;
        } elseif (isset($this->translated_tokens[$type])) {
            return $this->translated_tokens[$type];
        } elseif (isset($this->ignored_tokens[$type])) {
            return null;
        } elseif (isset($this->token_callback[$type])) {
            return call_user_func(array($this, $type), $t);
        } else {
            echo "Unknown token:\n";
            var_dump($t, $type, $value);
            die;
        }
    }
    
    public function get_tokens($tmp_file)
    {
        $py_file = dirname(__FILE__) . "/../../python/snow/lexer/lex-to-json.py";
        $json = shell_exec((("python " . $py_file) . " ") . $tmp_file);
        $python_tokens = json_decode($json, true);
        if (!$python_tokens) {
            var_dump($json);
            die;
        }
        $debug = array();
        $php_tokens = array(array(T_OPEN_TAG, '<?php ', 1));
        foreach ($python_tokens as $t) {
            $first = true;
            foreach ((array) $this->translate_token($t) as $php_token) {
                if ($php_token !== null) {
                    $php_tokens[] = $php_token;
                    $out_type = is_array($php_token) ? snow_token_name($php_token[0]) : 'LITERAL';
                    $out_value = is_array($php_token) ? $php_token[1] : $php_token;
                    $debug[] = array('in_type' => $first ? $t['type'] : '', 'in_value' => $first ? $t['value'] : '', 'out_type' => $out_type, 'out_value' => $out_value);
                    $first = false;
                }
            }
            unset($php_token);
        }
        unset($t);
        return array($php_tokens, $debug);
    }
    
    public function T_STRING_WITH_CONCAT($t)
    {
        return array(array(T_STRING_DOUBLE, $t['value'], 2), ".");
    }
    
    public function T_NUMBER($t)
    {
        if (is_float($t['value'][0])) {
            return array(array(T_DNUMBER, $t['value'][1], 2));
        } else {
            return array(array(T_LNUMBER, $t['value'][1], 2));
        }
    }

    public function __construct($code) {
        $this->tokens = array();
        $this->debug = array();
        $this->named_tokenmap = get_named_tokenmap();
        $this->transform_token_value = array('T_VARIABLE' => '$%s');
        $this->literal_tokens = array('T_PLUS' => 1, 'T_GREATER' => 1, 'T_LPAR' => 1, 'T_RPAR' => 1, 'T_MINUS' => 1, 'T_STAR' => 1, 'T_SLASH' => 1, 'T_EQUAL' => 1, 'T_AMPER' => 1, 'T_COMMA' => 1, 'T_LSQB' => 1, 'T_RSQB' => 1, 'T_QUESTION_MARK' => 1, 'T_COLON' => 1);
        $this->translated_tokens = array('T_NEWLINE' => ';', 'T_INDENT' => '{', 'T_DEDENT' => '}', 'T_BAND' => '&', 'T_BXOR' => '^', 'T_PERCENT' => '.', 'T_MOD' => '%', 'T_BNOT' => '~', 'T_BOR' => '|', 'T_LBRACE' => '{', 'T_RBRACE' => '}', 'T_LESS' => '<', 'T_NOT' => '!');
        $this->ignored_tokens = array('T_ENDMARKER' => 1, 'T_PASS' => 1);
        $this->token_types_map = array('T_NAME' => 'T_VARIABLE', 'T_PHP_STRING' => 'T_STRING', 'T_BLEFT' => 'T_SL', 'T_BRIGHT' => 'T_SR', 'T_FN' => 'T_FUNCTION', 'T_DOUBLE_DOT' => 'T_PAAMAYIM_NEKUDOTAYIM', 'T_CALLABLE' => 'T_STRING', 'T_TRUE' => 'T_STRING', 'T_FALSE' => 'T_STRING', 'T_ELIF' => 'T_ELSEIF', 'T_ISA' => 'T_INSTANCEOF', 'T_DIE' => 'T_EXIT', 'T_OR' => 'T_BOOLEAN_OR', 'T_XOR' => 'T_LOGICAL_XOR', 'T_AND' => 'T_BOOLEAN_AND', 'T__OR_' => 'T_LOGICAL_OR', 'T__AND_' => 'T_LOGICAL_AND', 'T_DOT' => 'T_OBJECT_OPERATOR', 'T_NULL' => 'T_STRING', 'T_CONSTANT_NAME' => 'T_STRING', 'T_CLASS_NAME' => 'T_STRING', 'T_FLOAT_CAST' => 'T_DOUBLE_CAST', 'T_STRINGTYPE_CAST' => 'T_STRING_CAST', 'T_NEXT' => 'T_CONTINUE', 'T_PARENT' => 'T_STRING');
        $this->token_callback = array('T_STRING_WITH_CONCAT' => 1, 'T_NUMBER' => 1);
        $tmp_file = "/tmp/.snowcode";
        file_put_contents($tmp_file, $code);
        parent::__construct("");
        list($this->tokens, $this->debug) = $this->get_tokens($tmp_file);
        unlink($tmp_file);
    }

}
