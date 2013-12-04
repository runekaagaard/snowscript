<?php
function insert($into, $item)
{
    array_unshift($into, $item);
}
function rpl($string, $search, $replace)
{
    return str_replace($search, $replace, $string);
}
function slice($guy, $start, $stop = null)
{
    if (!$stop) {
        if (\snow_lt($start, 0)) {
            $n = count($guy) - 1;
            $x = $guy[$n - $start];
            return $x;
        } else {
            throw new Exception("todo");
        }
    } else {
        throw new Exception("todo");
    }
}
function append($guy, $kv, $v = null)
{
    if ($v) {
        $guy[$kv] = $v;
    } else {
        $guy[] = $v;
    }
}
function lget($guy)
{
    
}
class Snowscript_Visitors_Scope extends PHPParser_NodeVisitorAbstract
{
    
    public function __construct()
    {
        $this->empty_scope = snow_list(array('imports' => snow_list(array())));
        $this->root_scope = snow_list(array());
        $this->scopes = snow_list(array($this->empty_scope));
        $this->scope = $this->scopes[0];
    }
    
    public function enterNode(PHPParser_Node $node)
    {
        if (($node instanceof PHPParser_Node_Stmt_Imports)) {
            $this->add_imports($node);
        }
        if (($node instanceof PHPParser_Node_Expr_Assign)) {
            append(lget($this->scopes, -1), snow_list(array("hej")));
        }
    }
    
    public function add_imports($node)
    {
        $paths = snow_list(array());
        foreach ($node->import_paths as $import_path) {
            $paths[] = $import_path->name;
        }
        unset($import_path);
        $prefix = join($paths, "__");
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
    }
    
    public function afterTraverse(array $nodes)
    {
        return $nodes;
    }
}
