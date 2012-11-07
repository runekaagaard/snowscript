<?php

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
        parent::__construct(
            array(
                'value' => $value,
                'string_type' => $string_type,
            ),
            $line, $docComment
        );
    }
}