<?php

/**
 * @property null|PHPParser_Node_Expr $cond  Condition (null for default)
 * @property PHPParser_Node[]         $stmts Statements
 */
class PHPParser_Node_Stmt_Case extends PHPParser_NodeAbstract
{
    /**
     * Constructs a case node.
     *
     * @param null|PHPParser_Node_Expr $conds      Conditions
     * @param PHPParser_Node[]         $stmts      Statements
     * @param int                      $line       Line
     * @param null|string              $docComment Nearest doc comment
     */
    public function __construct(array $conds, array $stmts = array(), $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'conds'  => $conds,
                'stmts' => $stmts,
            ),
            $line, $docComment
        );
    }
}