<?php

define('T_IN', 1001);
define('T_TO', 1002);
define('T_DOWNTO', 1003);
define('T_STEP', 1004);
define('T_THEN', 1005);

// TODO: This is WIP code, just making a couple of tests pass.

function snow_token_name($i) {
    if ($i === T_IN) return 'T_IN';
    if ($i === T_TO) return 'T_TO';
    if ($i === T_DOWNTO) return 'T_DOWNTO';
    if ($i === T_STEP) return 'T_STEP';
    if ($i === T_THEN) return 'T_THEN';
    return token_name($i);
}

class Snowscript_Lexer extends PHPParser_Lexer {
    public $tokens = array();
    public $debug = array();

    function __construct($code) {
        $tmp_file = "/tmp/.snowcode";
        file_put_contents($tmp_file, $code);
        parent::__construct("");
        #var_dump(Snowscript_Lexer::$named_tokenmap,
        #         Snowscript_Lexer::$tokenMap); die;
        list($this->tokens, $this->debug) = $this->get_tokens($tmp_file);
        unlink($tmp_file);
    }

    public $transform_token_value = array(
        'T_VARIABLE' => '$%s', 'T_CONSTANT_ENCAPSED_STRING' => '"%s"',
    );

    // Use the actual code value of these.
    public $literal_tokens = array(
        'T_PLUS'=>1, 'T_GREATER'=>1, 'T_LPAR'=>1, 'T_RPAR'=>1,
        'T_MINUS'=>1, 'T_STAR'=>1, 'T_SLASH'=>1, 'T_EQUAL'=>1,
        'T_AMPER'=>1, 'T_COMMA'=>1, 'T_LSQB'=>1, 'T_RSQB'=>1, 
        'T_QUESTION_MARK'=>1, 'T_COLON'=>1,
    );
    // Use the value of the token names key.
    public $translated_tokens = array(
        'T_NEWLINE' => ';', 'T_INDENT' => '{', 'T_DEDENT' => '}',
        'T_BAND' => '&', 'T_BXOR' => '^', 'T_PERCENT' => '.', 'T_MOD' => '%',
        'T_BNOT' => '~', 'T_BOR' => '|', 'T_LBRACE' => '{',
        'T_RBRACE' => '}', 'T_LESS' => '<', 'T_NOT' => '!',
    );
    // Don't do anything with these.
    public $ignored_tokens = array(
        'T_ENDMARKER'=>1, 'T_PASS'=>1,
    );
    // Change the type of the token.
    public $token_types_map = array(
        'T_NAME' => 'T_VARIABLE',
        'T_PHP_STRING' => 'T_STRING',
        'T_BLEFT' => 'T_SL', 'T_BRIGHT' => 'T_SR',
        'T_STRING' => 'T_CONSTANT_ENCAPSED_STRING',
        'T_FN' => 'T_FUNCTION',
        'T_DOUBLE_COLON' => 'T_PAAMAYIM_NEKUDOTAYIM',
        'T_CALLABLE' => 'T_STRING',
        'T_TRUE' => 'T_STRING',
        'T_FALSE' => 'T_STRING',
        'T_ELIF' => 'T_ELSEIF',
        'T_ISA' => 'T_INSTANCEOF',
        'T_DIE' => 'T_EXIT',
        'T_OR' => 'T_BOOLEAN_OR',
        'T_XOR' => 'T_LOGICAL_XOR',
        'T_AND' => 'T_BOOLEAN_AND',
        'T__OR_' => 'T_LOGICAL_OR',
        'T__AND_' => 'T_LOGICAL_AND',
        'T_DOT' => 'T_OBJECT_OPERATOR',
        'T_NULL' => 'T_STRING',
        'T_CONSTANT_NAME' => 'T_STRING',
        'T_CLASS_NAME' => 'T_STRING',
        'T_FLOAT_CAST' => 'T_DOUBLE_CAST',
        'T_STRINGTYPE_CAST' => 'T_STRING_CAST',
        'T_NEXT' => 'T_CONTINUE',
     );
    public $token_callback = array(
        'T_STRING_WITH_CONCAT'=>1, 'T_NUMBER' =>1,
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
        $py_file = dirname(__FILE__) . '/../../python/snow/lexer/lex-to-json.py';
        $json = `python $py_file $tmp_file`;
        $python_tokens = json_decode($json, true);
        if (!$python_tokens) {
            var_dump($json);
            die;
        }
        $debug = array();
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
                            ? snow_token_name($php_token[0]) : 'LITERAL',
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
        for ($i = 256; $i < 1100; ++$i) {
            // T_DOUBLE_COLON is equivalent to T_PAAMAYIM_NEKUDOTAYIM
            if (T_DOUBLE_COLON === $i) {
                self::$named_tokenmap[$i] = 'T_PAAMAYIM_NEKUDOTAYIM';
            // T_OPEN_TAG_WITH_ECHO with dropped T_OPEN_TAG results in T_ECHO
            } elseif(T_OPEN_TAG_WITH_ECHO === $i) {
                self::$named_tokenmap[$i] = PHPParser_Parser::T_ECHO;
            // T_CLOSE_TAG is equivalent to ';'
            } elseif(T_CLOSE_TAG === $i) {
                self::$named_tokenmap[$i] = ord(';');
            // and the others can be mapped directly
            } elseif ('UNKNOWN' !== ($name = snow_token_name($i))
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
                T_CONSTANT_ENCAPSED_STRING,
                "'" . $t['value'] . "'",
                2
            ),
            ".",
        );
    }

    function T_NUMBER($t) {
        if (is_float($t['value'][0]))
            return array(array(T_DNUMBER, $t['value'][1], 2));
        else
            return array(array(T_LNUMBER, $t['value'][1], 2));

    }
}
Snowscript_Lexer::init_named_tokenmap();
#var_dump(Snowscript_Lexer::$named_tokenmap); die;
