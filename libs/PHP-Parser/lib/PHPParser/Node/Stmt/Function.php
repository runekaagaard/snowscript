<?php

/**
 * @property bool                   $byRef  Whether returns by reference
 * @property string                 $name   Name
 * @property PHPParser_Node_Param[] $params Parameters
 * @property PHPParser_Node[]       $stmts  Statements
 */
class PHPParser_Node_Stmt_Function extends PHPParser_Node_Stmt
{
    /**
     * Constructs a function node.
     *
     * @param string      $name       Name
     * @param array       $subNodes   Array of the following optional subnodes:
     *                                'byRef'  => false  : Whether to return by reference
     *                                'params' => array(): Parameters
     *                                'stmts'  => array(): Statements
     * @param int         $line       Line
     * @param null|string $docComment Nearest doc comment
     */
    public function __construct($name, array $subNodes = array(), $line = -1, $docComment = null) {
        parent::__construct(
            $subNodes + array(
                'byRef'  => false,
                'params' => array(),
                'stmts'  => array(),
            ),
            $line, $docComment
        );
        $this->name = $name;
    }

    function add_global_var($name) {
        if ($this->global_vars === null) {
            $this->global_vars = new PHPParser_Node_Stmt_Global(array());
        }
        $this->global_vars->vars[$name] = new PHPParser_Node_Expr_Variable($name);
    }

}