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
    public function __construct($type, $props, $line = -1, $docComment = null) {
        $args = func_get_args();
        var_dump("ARGS propdecl", $args, "###################");
        parent::__construct(
            array(
                'type'  => $type,
                'props' => array(),
            ),
            $line, $docComment
        );
    }
}