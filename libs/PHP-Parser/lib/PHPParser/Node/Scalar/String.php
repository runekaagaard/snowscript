<?php

function phpparser_node_scalar_string_replace_single($match) {
    return str_replace("'", "\\'", $match[0]);
}

function phpparser_node_scalar_string_replace_double($match) {
    return str_replace('"', '\\"', $match[0]);
}

/**
 * @property string $value String value
 */
class PHPParser_Node_Scalar_String extends PHPParser_Node_Scalar
{
    /**
     * Constructs a string scalar node.
     *
     * @param string      $value      Value of the string
     * @param int         $line       Line
     * @param null|string $docComment Nearest doc comment
     */
    public function __construct($value = '', $string_type, $line = -1, $docComment = null) {
        if ($string_type === "'") 
            $value = preg_replace_callback("#[^\\\][']+?#Uis", 
                'phpparser_node_scalar_string_replace_single', $value);
        elseif ($string_type === '"')
            $value = preg_replace_callback('#[^\\\]"+?#Uis', 
                'phpparser_node_scalar_string_replace_double', $value);
        else
            throw new Exception("Unsupported string type.");
        parent::__construct(
            array(
                'value' => $value,
                'string_type' => $string_type,
            ),
            $line, $docComment
        );
    }
}