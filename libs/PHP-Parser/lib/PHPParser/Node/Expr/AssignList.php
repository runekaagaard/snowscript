<?php

// TODO: Document and cleanup.

/**
 * @property array               $vars List of variables to assign to
 * @property PHPParser_Node_Expr $expr Expression
 */
class PHPParser_Node_Expr_AssignList extends PHPParser_Node_Expr
{
    /**
     * Constructs a list() assignment node.
     *
     * @param array               $vars       List of variables to assign to
     * @param PHPParser_Node_Expr $expr       Expression
     * @param int                 $line       Line
     * @param null|string         $docComment Nearest doc comment
     */
    public function __construct(array $vars, PHPParser_Node_Expr $expr, $line = -1, $docComment = null) {
        parent::__construct(
            array(
                'vars' => $this->processVars($vars),
                'expr' => $expr
            ),
            $line, $docComment
        );
    }

    private function processVars(array &$vars) {
        $traverser = new PHPParser_NodeTraverser;
        $traverser->addVisitor(new Snow_AssignList_Visitor);
        return $traverser->traverse($vars);

    }
}

class PHPParser_Node_Expr_AssignListInner extends PHPParser_Node_Expr {
    public function __construct($vars, $line = -1, $docComment = null) {
        parent::__construct(
            array('vars' => $vars),
            $line, $docComment
        );
    }    

}

class PHPParser_Node_Expr_Pass extends PHPParser_Node_Expr {}

class Snow_AssignList_Visitor extends PHPParser_NodeVisitorAbstract {
    const keyFound = "Unexpected key found in destructuring expresion";
    const byRefFound = "Unexpected '&' found in destructuring expresion";
    const unexpectedTokenFound = "Unexpected node %s found in destructuring expresion";
    public function leaveNode(PHPParser_Node $node) {
        if ($node instanceof PHPParser_Node_Expr_Array) {
            return new PHPParser_Node_Expr_AssignListInner(
                $node->getSubNodes());
        } elseif ($node instanceof PHPParser_Node_Expr_Variable ||
        $node instanceof PHPParser_Node_Expr_ConstFetch) {
            return null;
        } elseif ($node instanceof PHPParser_Node_Name &&
        $node->parts[0] === "null") {
            return new PHPParser_Node_Expr_Pass(array());

        } elseif ($node instanceof PHPParser_Node_Expr_ArrayItem) {
            if ($node->key !== null)
                throw new PHPParser_Error(self::keyFound, $node->getLine());
            if ($node->byRef !== false)
                throw new PHPParser_Error(self::byRefFound, $node->getLine());
        } else {
            return $node;
        }
    }
}