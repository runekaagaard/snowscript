<?php

/**
 * @property null|PHPParser_Node_Expr $num Number of loops to continue
 */
class PHPParser_Node_Stmt_Import extends PHPParser_Node_Stmt
{
    /**
     * Constructs a continue node.
     *
     * @param null|PHPParser_Node_Expr $num        Number of loops to continue
     * @param int                      $line       Line
     * @param null|string              $docComment Nearest doc comment
     */
    public function __construct($name, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'name' => ltrim($name, '$'),
            ),
            $line, $docComment
        );
    }
}