<?php

class Snowscript_Node_Stmt_Implements extends PHPParser_Node_Stmt
{
    /**
     * Constructs an extends node.
     *
     * @param PHPParser_Node_Expr $cond       Condition
     * @param PHPParser_Node[]    $stmts      Statements
     * @param int                 $line       Line
     * @param null|string         $docComment Nearest doc comment
     */
    public function __construct(array $names, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'names'  => $names,
            ),
            $line, $docComment
        );
    }
}