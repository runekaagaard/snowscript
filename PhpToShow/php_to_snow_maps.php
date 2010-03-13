<?php
function get_php_to_snow_map() {
    static $map = array(
        // Tokens
        'T_OPEN_TAG' => PhpToSnow::PARSE_WITH_METHOD,
        'T_VARIABLE' => PhpToSnow::PARSE_WITH_METHOD,
        'T_STRING' => PhpToSnow::PARSE_UNCHANGED,
        'T_FOREACH' => PhpToSnow::PARSE_WITH_METHOD,
        'T_WHITESPACE' => PhpToSnow::PARSE_WITH_METHOD,
        'T_AS' => PhpToSnow::PARSE_UNCHANGED,
        'T_IF' => PhpToSnow::PARSE_WITH_METHOD,
        'T_ECHO' => PhpToSnow::PARSE_UNCHANGED,
        'T_LNUMBER' => PhpToSnow::PARSE_UNCHANGED,
        'T_CONSTANT_ENCAPSED_STRING' => PhpToSnow::PARSE_UNCHANGED,
        'T_ELSE' => PhpToSnow::PARSE_UNCHANGED,
        'T_ENCAPSED_AND_WHITESPACE' => PhpToSnow::PARSE_UNCHANGED,
        'T_FOR' => PhpToSnow::PARSE_WITH_METHOD,
        'T_DOUBLE_ARROW' => ',',
        'T_CLOSE_TAG' => '%>',
        'T_CURLY_OPEN' => '%',
        'T_INC' => PhpToSnow::PARSE_UNCHANGED,
        'T_PRINT' => PhpToSnow::PARSE_UNCHANGED,
        'T_FILE' => PhpToSnow::PARSE_UNCHANGED,
        'T_INLINE_HTML' => PhpToSnow::PARSE_UNCHANGED,
        'T_IS_IDENTICAL' => PhpToSnow::PARSE_UNCHANGED,

        // Unnamed tokens
        '"' => array(
                'action' => PhpToSnow::PARSE_WITH_METHOD,
                'name' => 'DOUBLEQUOTES'
               ),
        '(' => PhpToSnow::PARSE_UNCHANGED,
        ')' => PhpToSnow::PARSE_UNCHANGED,
        '[' => PhpToSnow::PARSE_UNCHANGED,
        ']' => PhpToSnow::PARSE_UNCHANGED,
        '{' => array(
                'action' => PhpToSnow::PARSE_WITH_METHOD,
                'name' => 'LBRACKET'
               ),
        '}' => array(
                'action' => PhpToSnow::PARSE_WITH_METHOD,
                'name' => 'RBRACKET'
               ),
        ';' => array(
                'action' => PhpToSnow::PARSE_WITH_METHOD,
                'name' => 'SEMICOLON'
               ),
        '.' => '^',
        '=' => PhpToSnow::PARSE_UNCHANGED,
        '+' => PhpToSnow::PARSE_UNCHANGED,
        '-' => PhpToSnow::PARSE_UNCHANGED,
        '*' => PhpToSnow::PARSE_UNCHANGED,
        '/' => PhpToSnow::PARSE_UNCHANGED,
        '|' => PhpToSnow::PARSE_UNCHANGED,
        '&' => PhpToSnow::PARSE_UNCHANGED,
        '!' => PhpToSnow::PARSE_UNCHANGED,
        '<' => PhpToSnow::PARSE_UNCHANGED,
        '>' => PhpToSnow::PARSE_UNCHANGED,
        ',' => PhpToSnow::PARSE_UNCHANGED,
    );
    return $map;
}