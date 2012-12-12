<?php

class Snowscript_Node_Stmt_Extends extends PHPParser_Node_Stmt
{
    /**
     * Constructs an extends node.
     *
     * @param PHPParser_Node_Expr $cond       Condition
     * @param PHPParser_Node[]    $stmts      Statements
     * @param int                 $line       Line
     * @param null|string         $docComment Nearest doc comment
     */
    public function __construct(PHPParser_Node_Name $name, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'name'  => $name,
            ),
            $line, $docComment
        );
    }
}