<?php

/**
 * @property int                                    $type  Modifiers
 * @property PHPParser_Node_Stmt_PropertyProperty[] $props Properties
 */
class PHPParser_Node_Stmt_PropertyDeclarations extends PHPParser_Node_Stmt
{
    /**
     * Constructs a class property list node.
     *
     * @param int                                    $type       Modifiers
     * @param PHPParser_Node_Stmt_PropertyProperty[] $props      Properties
     * @param int                                    $line       Line
     * @param null|string                            $docComment Nearest doc comment
     */
    public function __construct(array $tree, $line = -1, $docComment = null) {
        $modifiers = array();
        while (gettype($tree[1]) === 'array') {
            $modifiers []= $tree[0];
            $tree = $tree[1];
        }
        $modifiers []= $tree[0];
        parent::__construct(
            array(
                'modifiers' => $modifiers,
                'stmts' => $tree[1],
            ),
            $line, $docComment
        );
    }
}