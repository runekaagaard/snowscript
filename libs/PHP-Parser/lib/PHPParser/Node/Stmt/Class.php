<?php

class PHPParser_Node_Stmt_Class_Traverse extends PHPParser_NodeVisitorAbstract {
    public $parameter_names = array();
    public $class_node;

    public function __construct($class_node) {
        $this->class_node = $class_node;
        foreach ($class_node->parameter_list as $node) {
            $this->parameter_names [$node->name]= true;
        }
    }

    public function leaveNode(PHPParser_Node $node) {
        if ($node instanceof PHPParser_Node_Expr_Variable) {
            $in_props = isset($this->class_node->prop_names[$node->name]);
            $in_params = isset($this->parameter_names[$node->name]);

            if (!$in_props and $in_params) return;
            
            return new PHPParser_Node_Expr_PropertyFetch(
                new PHPParser_Node_Expr_Variable('this'),
                $node->name
            );
        }
    }
}

/**
 * @property int                      $type       Type
 * @property string                   $name       Name
 * @property null|PHPParser_Node_Name $extends    Name of extended class
 * @property PHPParser_Node_Name[]    $implements Names of implemented interfaces
 * @property PHPParser_Node[]         $stmts      Statements
 */

class PHPParser_Node_Stmt_Class extends PHPParser_Node_Stmt
{
    const MODIFIER_PUBLIC    =  1;
    const MODIFIER_PROTECTED =  2;
    const MODIFIER_PRIVATE   =  4;
    const MODIFIER_STATIC    =  8;
    const MODIFIER_ABSTRACT  = 16;
    const MODIFIER_FINAL     = 32;

    protected static $specialNames = array(
        'self'   => true,
        'parent' => true,
        'static' => true,
    );

    /**
     * Constructs a class node.
     *
     * @param string      $name       Name
     * @param array       $subNodes   Array of the following optional subnodes:
     *                                'type'       => 0      : Type
     *                                'extends'    => null   : Name of extended class
     *                                'implements' => array(): Names of implemented interfaces
     *                                'stmts'      => array(): Statements
     * @param int         $line       Line
     * @param null|string $docComment Nearest doc comment
     */
    public function __construct($name, array $subNodes = array(), $line = -1, $docComment = null) {
        parent::__construct(
            $subNodes + array(
                'type'       => 0,
                'extends'    => null,
                'implements' => array(),
                'stmts'      => array(),
                'props'      => array(),
            ),
            $line, $docComment
        );
        $this->name = $name;

        if (isset(self::$specialNames[(string) $this->name])) {
            throw new PHPParser_Error(sprintf('Cannot use "%s" as class name as it is reserved', $this->name));
        }

        if (isset(self::$specialNames[(string) $this->extends])) {
            throw new PHPParser_Error(sprintf('Cannot use "%s" as class name as it is reserved', $this->extends));
        }

        foreach ($this->implements as $interface) {
            if (isset(self::$specialNames[(string) $interface])) {
                throw new PHPParser_Error(sprintf('Cannot use "%s" as interface name as it is reserved', $interface));
            }
        }
        list($this->stmts, $this->props) = $this->splitStmtsAndProps($this->stmts);
    }

    private function splitStmtsAndProps($stmts_old) {
        $stmts = array();
        $props = array();
        $this->prop_names = array();
        $traverser = new PHPParser_NodeTraverser;
        $traverser->addVisitor(new PHPParser_Node_Stmt_Class_Traverse($this));
        foreach ($stmts_old as $stmt) {
            if ($stmt instanceof PHPParser_Node_Expr_AssignClassProperty) {
                $stmts_traversed = $traverser->traverse(array($stmt));
                $stmt = $stmts_traversed[0];
                $props []= $stmt;
                $this->prop_names[$stmt->var->name]= true;
            } else {
                $stmts []= $stmt;
            }
        }
        $this->prop_names = array();
        return array($stmts, $props);
    }

    public static function verifyModifier($a, $b) {
        if ($a & 7 && $b & 7) {
            throw new PHPParser_Error('Multiple access type modifiers are not allowed');
        }

        if ($a & self::MODIFIER_ABSTRACT && $b & self::MODIFIER_ABSTRACT) {
            throw new PHPParser_Error('Multiple abstract modifiers are not allowed');
        }

        if ($a & self::MODIFIER_STATIC && $b & self::MODIFIER_STATIC) {
            throw new PHPParser_Error('Multiple static modifiers are not allowed');
        }

        if ($a & self::MODIFIER_FINAL && $b & self::MODIFIER_FINAL) {
            throw new PHPParser_Error('Multiple final modifiers are not allowed');
        }

        if ($a & 48 && $b & 48) {
            throw new PHPParser_Error('Cannot use the final and abstract modifier at the same time');
        }
    }
}
