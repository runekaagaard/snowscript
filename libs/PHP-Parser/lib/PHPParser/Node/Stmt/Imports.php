<?php

/**
 * @property null|PHPParser_Node_Expr $num Number of loops to continue
 */
class PHPParser_Node_Stmt_Imports extends PHPParser_Node_Stmt
{
    /**
     * Constructs a continue node.
     *
     * @param null|PHPParser_Node_Expr $num        Number of loops to continue
     * @param int                      $line       Line
     * @param null|string              $docComment Nearest doc comment
     */
    public function __construct(array $import_paths, array $imports, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'import_paths' => $import_paths,
                'imports' => $imports,
            ),
            $line, $docComment
        );
    }
}