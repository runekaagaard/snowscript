<?php
function rpl($string, $search, $replace)
{
    return str_replace($search, $replace, $string);
};
function sjoin($guys, $separator = ", ")
{
    return join((array) $guys, $separator);
};
function type($guy)
{
    return gettype($guy);
};
class Snowscript_Visitors_Scope extends PHPParser_NodeVisitorAbstract
{
    
    public function __construct()
    {
        $this->empty_scope = snow_dict(array('imports' => snow_dict(array()), 'assigns' => snow_dict(array()), 'fns' => snow_dict(array())));
        $this->scopes = snow_list(array($this->empty_scope));
    }
    
    public function enterNode(PHPParser_Node $node)
    {
        $this->scope = $this->scopes[-1];
        if (($node instanceof PHPParser_Node_Stmt_Imports)) {
            $this->add_imports($node);
        } elseif (($node instanceof PHPParser_Node_Expr_Assign)) {
            $this->scopes[-1]['assigns'][$node->var->name] = $node;
        } elseif (($node instanceof PHPParser_Node_Stmt_Function)) {
            $last_scope = $this->scopes[-1];
            $this->scopes->append($last_scope->copy());
            $this->scopes[-1]['fns'][$node->name] = $node;
        }
    }
    
    public function add_imports($node)
    {
        $paths = snow_list(array());
        foreach ($node->import_paths as $import_path) {
            $paths[] = $import_path->name;
        }
        unset($import_path);
        $prefix = sjoin($paths, "__");
        foreach ($node->imports as $imp) {
            $this->scopes[0]['imports'][$imp->name] = ($prefix . "__") . $imp->name;
        }
        unset($imp);
    }
    
    public function leaveNode(PHPParser_Node $node)
    {
        if (($node instanceof PHPParser_Node_Stmt_Imports)) {
            return false;
        }
        if (($node instanceof PHPParser_Node_Stmt_Function)) {
            $this->scopes->pop();
        }
    }
    
    public function afterTraverse(array $nodes)
    {
        return $nodes;
    }
}
