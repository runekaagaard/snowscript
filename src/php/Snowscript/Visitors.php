<?php
function rpl($string, $search, $replace)
{
    return str_replace($search, $replace, $string);
};
function sjoin($l, $separator = ", ")
{
    return join($l->arr, $separator);
};
function type($guy)
{
    return gettype($guy);
};
function uu($l)
{
    return sjoin($l, '__');
};
function pp($obj, $indent = 0)
{
    foreach ($obj as $k => $v) {
        $type = gettype($v);
        if ((\snow_eq($type, "string") || \snow_eq($type, "integer")) || \snow_eq($type, "NULL")) {
            echo sprintf("%s%s: %s\n", str_repeat(" ", $indent * 2), (string) $k, (string) $v);
        } elseif (\snow_eq($type, "boolean")) {
            echo sprintf("%s%s: %s\n", str_repeat(" ", $indent * 2), (string) $k, $v ? "true" : "false");
        } elseif (\snow_eq($type, "object")) {
            echo sprintf("%s%s - %s\n", str_repeat(" ", $indent * 2), $k, get_class($v));
            pp($v, $indent + 1);
        } elseif (\snow_eq($type, "array")) {
            echo sprintf("%s%s - %s\n", str_repeat(" ", $indent * 2), $k, "array");
            pp($v, $indent + 1);
        } else {
            var_dump($type);
            throw new Exception("Type not implemented: " . $type);
        }
    }
    unset($k, $v);
};
function v($x)
{
    var_dump($x);
};
class Snowscript_Visitors_Scope extends PHPParser_NodeVisitorAbstract
{
    
    public function __construct($ns)
    {
        $this->ns = $ns;
        $this->scopes = snow_list(array(snow_dict(array('names' => snow_dict(array()), 'prefix' => snow_list(array())))));
        $this->func = null;
        $this->in_assign = false;
    }
    
    public function name_in_cur_scope($name)
    {
        return $this->scopes[-1]->names->get($name);
    }
    
    public function new_name_from_cur_scope($name)
    {
        return $this->scopes[-1]->names[$name]->new_name;
    }
    
    public function name_in_prev_scope($name)
    {
        try {
            return $this->scopes[-2]->names->get($name);
        } catch (IndexError $e) {
            return False;
        }
    }
    
    public function make_new_name_cur($name)
    {
        if (\snow_eq(count($this->scopes), 1)) {
            return uu(snow_list(array($this->ns, $name)));
        } else {
            return uu(snow_list(array(uu($this->scopes[-1]->prefix), $name)));
        }
    }
    
    public function make_new_name_prev($name)
    {
        return uu(snow_list(array(uu($this->scopes[-2]->prefix), $name)));
    }
    
    public function add_node_to_cur_scope($node, $name, $new_name, $is_global, $global_name)
    {
        if (!$this->scopes[-1]->names->get($name)) {
            $this->scopes[-1]->names[$name] = snow_dict(array('nodes' => snow_list(array()), 'new_name' => null, 'is_global' => null, 'global_name' => $global_name));
        }
        $this->scopes[-1]->names[$name]->is_global = $is_global;
        $this->scopes[-1]->names[$name]->nodes->append($node);
    }
    
    public function rename_nodes($name, $new_name, $scope_index)
    {
        $this->scopes[$scope_index]->names[$name]->new_name = $new_name;
        foreach ($this->scopes[$scope_index]->names[$name]->nodes as $node) {
            if (($node instanceof PHPParser_Node_Expr_Assign)) {
                $node->var->name = $new_name;
            } elseif (($node instanceof PHPParser_Node_Stmt_Function)) {
                $node->name = $new_name;
            } elseif (($node instanceof PHPParser_Node_Expr_FuncCall)) {
                $node->name->parts[0] = $new_name;
            } elseif (($node instanceof PHPParser_Node_Expr_Variable)) {
                $node->name = $new_name;
            } else {
                throw new Exception(get_class("Not supported: " . $node));
            }
        }
        unset($node);
    }
    
    public function rename_nodes_cur_scope($name, $new_name)
    {
        $this->rename_nodes($name, $new_name, -1);
    }
    
    public function rename_nodes_all_scopes($name, $new_name)
    {
        foreach ($this->scopes as $k => $scope) {
            if ($scope->names->get($name)) {
                $this->rename_nodes($name, $new_name, $k);
            }
        }
        unset($k, $scope);
    }
    
    public function name_in_prev_is_global($name)
    {
        return $this->scopes[-2]->names[$name]->is_global;
    }
    
    public function mark_prev_scope_global($name)
    {
        foreach ($this->scopes as $scope) {
            try {
                $scope->names[$name]->is_global = true;
            } catch (KeyError $e) {
                
            }
        }
        unset($scope);
    }
    
    public function get_global_name_prev($name)
    {
        return $this->scopes[-2]->names[$name]->global_name;
    }
    
    public function create_name($node, $name, $locked)
    {
        $is_global = false;
        $global_name = $this->make_new_name_cur($name);
        if ($this->name_in_prev_scope($name)) {
            if ($locked) {
                throw new Exception("Cant redefine name from outer scope: " . $name);
            }
            $is_global = true;
            if ($this->name_in_prev_is_global($name)) {
                $new_name = $this->get_global_name_prev($name);
            } else {
                $new_name = $this->get_global_name_prev($name);
                $this->mark_prev_scope_global($name);
                $this->rename_nodes_all_scopes($name, $new_name);
            }
        } elseif ($this->name_in_cur_scope($name)) {
            if ($locked) {
                throw new Exception("Cant redefine name from same scope: " . $name);
            }
            $new_name = $this->new_name_from_cur_scope($name);
        } else {
            if (\snow_eq(count($this->scopes), 1)) {
                $new_name = uu(snow_list(array($this->ns, $name)));
            } else {
                $new_name = $name;
            }
        }
        $this->add_node_to_cur_scope($node, $name, $new_name, $is_global, $global_name);
        $this->rename_nodes_cur_scope($name, $new_name);
    }
    
    public function add_name($node, $name)
    {
        $is_global = false;
        $global_name = $this->make_new_name_cur($name);
        if ($this->name_in_prev_scope($name)) {
            $is_global = true;
            if ($this->name_in_prev_is_global($name)) {
                $new_name = $this->get_global_name_prev($name);
            } else {
                $new_name = $this->get_global_name_prev($name);
                $this->mark_prev_scope_global($name);
                $this->rename_nodes_all_scopes($name, $new_name);
            }
        } elseif ($this->name_in_cur_scope($name)) {
            $new_name = $this->new_name_from_cur_scope($name);
        } else {
            throw new Exception("Variable doesn't exist: " . $name);
        }
        $this->add_node_to_cur_scope($node, $name, $new_name, $is_global, $global_name);
        $this->rename_nodes_cur_scope($name, $new_name);
    }
    
    public function enterNode(PHPParser_Node $node)
    {
        if (($node instanceof PHPParser_Node_Expr_Assign)) {
            $this->create_name($node, $node->var->name, false);
            $this->in_assign = true;
        } elseif (($node instanceof PHPParser_Node_Stmt_Function)) {
            $this->create_name($node, $node->name, true);
            $this->scopes->append($this->scopes[-1]->copy());
            $this->scopes[-1]->prefix->append($node->name);
        } elseif (($node instanceof PHPParser_Node_Expr_Variable)) {
            if (!$this->in_assign) {
                $this->create_name($node, $node->name, false);
            }
        } elseif (($node instanceof PHPParser_Node_Expr_FuncCall)) {
            if (\snow_eq(count($node->name->parts), 1)) {
                $this->create_name($node, $node->name->parts[0], false);
                $node->as_variable = true;
            }
        }
    }
    
    public function leaveNode(PHPParser_Node $node)
    {
        if (($node instanceof PHPParser_Node_Stmt_Imports)) {
            return false;
        }
        if (($node instanceof PHPParser_Node_Stmt_Function)) {
            $this->scopes->pop();
        }
        if (($node instanceof PHPParser_Node_Expr_Assign)) {
            $this->in_assign = false;
        }
    }
    
    public function afterTraverse(array $nodes)
    {
        return $nodes;
    }
    
    public function add_imports($node)
    {
        $paths = snow_list(array());
        foreach ($node->import_paths as $import_path) {
            $paths[] = $import_path->name;
        }
        unset($import_path);
        $prefix = uu($paths);
        foreach ($node->imports as $imp) {
            $this->scopes[0]['imports'][$imp->name] = ($prefix . "__") . $imp->name;
        }
        unset($imp);
    }
}
