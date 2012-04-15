<?php

// TODO: This is WIP code, just making a couple of tests pass.

class Snowscript_Lexer extends PHPParser_Lexer {
    public $tokens = array();
    public $debug = array();

    function __construct($code) {
        $tmp_file = "/tmp/.snowcode";
        file_put_contents($tmp_file, $code);
        parent::__construct("");
        list($this->tokens, $this->debug) = $this->get_tokens($tmp_file);
        unlink($tmp_file);
    }

    public $transform_token_value = array(
        'T_VARIABLE' => '$%s', 'T_CONSTANT_ENCAPSED_STRING' => "'%s'",
    );

    // Use the actual code value of these.
    public $literal_tokens = array(
        'T_PLUS'=>1, 'T_GREATER'=>1, 'T_LPAR'=>1, 'T_RPAR'=>1,
        'T_MINUS'=>1, 'T_STAR'=>1, 'T_SLASH'=>1, 'T_EQUAL'=>1,
        'T_AMPER'=>1, 'T_COMMA'=>1, 'T_LSQB'=>1, 'T_RSQB'=>1,
        'T_DOUBLE_COLON'=>1,
    );
    // Use the value of the token names key.
    public $translated_tokens = array(
        'T_NEWLINE' => ';', 'T_INDENT' => '{', 'T_DEDENT' => '}',
        'T_BAND' => '&', 'T_BXOR' => '^', 'T_PERCENT' => '.', 'T_MOD' => '%',
        'T_BNOT' => '~', 'T_BOR' => '|', 'T_DOT' => '->',
    );
    // Don't do anything with these.
    public $ignored_tokens = array(
        'T_ENDMARKER'=>1,
    );
    // Change the type of the token.
    public $token_types_map = array(
        'T_NUMBER' => 'T_LNUMBER', 'T_NAME' => 'T_VARIABLE', 
        'T_PHP_STRING' => 'T_STRING',
        'T_BLEFT' => 'T_SL', 'T_BRIGHT' => 'T_SR',
        'T_COLON' => 'T_DOUBLE_ARROW', 
        'T_STRING' => 'T_CONSTANT_ENCAPSED_STRING',
    );
    // Don't touch these.
    public $token_callback = array(
        'T_STRING_WITH_CONCAT'=>1,
    );

    function alter_token_type($t) {
        $type = $t['type'];
        $type = 'T_' . $type;
        if (isset($this->token_types_map[$type]))
            $type = $this->token_types_map[$type];
        return $type;
    }

    function alter_token_value($t, $altered_type) {
        $value = $t['value'];
        if (isset($this->transform_token_value[$altered_type]))
            $value = sprintf($this->transform_token_value[$altered_type], 
                             $value);
        return $value;
    }

    function translate_token($t) {
        $type = $this->alter_token_type($t);
        $value = $this->alter_token_value($t, $type);
        if (!empty(self::$named_tokenmap[$type])) {
            $token_number = self::$named_tokenmap[$type];
            return array(array(
                $token_number,
                (is_array($value) ? $value[1] : $value),
                2,
            ));
        } elseif (!empty($this->literal_tokens[$type])) {
            return $value;
        } elseif (!empty($this->translated_tokens[$type])) {
            return $this->translated_tokens[$type];
        } elseif (!empty($this->ignored_tokens[$type])) {
            return null;
        } elseif (isset($this->token_callback[$type])) {
            return $this->$type($t);
        } else {
            echo "Unknown token:\n";
            var_dump($t, $type, $value);
            die;
        }
    }

    function get_tokens($tmp_file) {
        $py_file = dirname(__FILE__) . '/../../python/snow/lexer/json-lex.py';
        $python_tokens = json_decode(`python $py_file $tmp_file`,
                                     true);
        $php_tokens = array(array(368, '<?php ', 1));
        foreach($python_tokens as $t) {
            $first = true;
            foreach ((array)$this->translate_token($t) as $php_token) {
                if ($php_token !== null)  {
                    $php_tokens []= $php_token;
                    $debug []= array(
                        'in_type' => $first ? $t['type'] : '',
                        'in_value' => $first ? $t['value'] : '',
                        'out_type' => is_array($php_token)
                            ? token_name($php_token[0]) : 'LITERAL',
                        'out_value' => is_array($php_token)
                            ? $php_token[1] : $php_token,
                    );
                    $first = false;
                }
            }
        }
        return array($php_tokens, $debug);
    }

    /**
     * Verbatim copy of initTokenMap from PHP-Parser's PHPParser_Lexer, only
     * using the token name as key, instead of the token number.
     */
    
    static $named_tokenmap = array();
    static function init_named_tokenmap() {
        // 256 is the minimum possible token number, as everything below
        // it is an ASCII value
        for ($i = 256; $i < 1000; ++$i) {
            // T_DOUBLE_COLON is equivalent to T_PAAMAYIM_NEKUDOTAYIM
            if (T_DOUBLE_COLON === $i) {
                self::$named_tokenmap[$i] = PHPParser_Parser::T_PAAMAYIM_NEKUDOTAYIM;
            // T_OPEN_TAG_WITH_ECHO with dropped T_OPEN_TAG results in T_ECHO
            } elseif(T_OPEN_TAG_WITH_ECHO === $i) {
                self::$named_tokenmap[$i] = PHPParser_Parser::T_ECHO;
            // T_CLOSE_TAG is equivalent to ';'
            } elseif(T_CLOSE_TAG === $i) {
                self::$named_tokenmap[$i] = ord(';');
            // and the others can be mapped directly
            } elseif ('UNKNOWN' !== ($name = token_name($i))
                      && defined($name = 'PHPParser_Parser::' . $name)
            ) {
                self::$named_tokenmap[$i] = $name;
            }
        }

        self::$named_tokenmap = array_flip(array_map(function($x) {
            return str_replace("PHPParser_Parser::", "", $x);
        }, self::$named_tokenmap));
    }

    function T_STRING_WITH_CONCAT($t) {
        return array(
            array(
                self::$named_tokenmap['T_CONSTANT_ENCAPSED_STRING'], 
                "'" . $t['value'] . "'", 
                2
            ),
            ".",
        );
    }
}
Snowscript_Lexer::init_named_tokenmap();