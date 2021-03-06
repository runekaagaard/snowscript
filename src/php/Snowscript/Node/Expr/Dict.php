<?php

/**
 * @property PHPParser_Node_Expr_DictItem[] $items Items
 */
class Snowscript_Node_Expr_Dict extends PHPParser_Node_Expr
{
    /**
     * Constructs an array node.
     *
     * @param PHPParser_Node_Expr_DictItem[] $items      Items of the array
     * @param int                             $line       Line
     * @param null|string                     $docComment Nearest doc comment
     */
    public function __construct(array $items = array(), $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'items' => $items
            ),
            $line, $docComment
        );
    }
}