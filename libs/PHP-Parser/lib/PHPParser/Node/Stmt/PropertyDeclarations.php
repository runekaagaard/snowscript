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
     * @param array                                  $tree     Tree
     * @param int                                    $line       Line
     * @param null|string                            $docComment Nearest doc comment
     */
    public function __construct(array $tree, $line = -1, $docComment = null) {
        $modifier = 0;
        while (is_array($tree[1])) {
            PHPParser_Node_Stmt_Class::verifyModifier($modifier, $tree[0]);
            $modifier |= $tree[0];
            $tree = $tree[1];
        }
        if (is_int($tree[0])) {
            PHPParser_Node_Stmt_Class::verifyModifier($modifier, $tree[0]);
            $modifier |= $tree[0];
        }
        
        parent::__construct(
            array(
                'modifier' => $modifier,
                'stmts' => isset($tree[1]->stmts) ? $tree[1]->stmts 
                                                  : array($tree),
            ),
            $line, $docComment
        );
    }
}
