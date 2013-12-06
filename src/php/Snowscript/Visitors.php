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
class Snowscript_Visitors_Scope extends PHPParser_NodeVisitorAbstract
{
    
    public function __construct($ns)
    {
        $this->empty_scope = snow_dict(array('imports' => snow_dict(array()), 'vars' => snow_dict(array()), 'fns' => snow_dict(array()), 'prefixes' => snow_list(array($ns))));
        $this->scopes = snow_list(array($this->empty_scope));
        $this->func = null;
    }
    
    public function enterNode(PHPParser_Node $node)
    {
        $scope = $this->scopes[-1];
        if (($node instanceof PHPParser_Node_Stmt_Imports)) {
            $this->add_imports($node);
        } elseif (($node instanceof PHPParser_Node_Expr_Assign)) {
            if (!$scope->vars->get($node->var->name)) {
                $scope->vars[$node->var->name] = snow_dict(array('prefix' => uu($scope->prefixes), 'nodes' => snow_list(array())));
            }
            $scope->vars[$node->var->name]->nodes->append($node);
        } elseif (($node instanceof PHPParser_Node_Stmt_Function)) {
            $this->func = $node;
            $scope['fns'][$node->name] = $node;
            $this->scopes->append($scope->copy());
            $scope = $this->scopes[-1];
            $scope->prefixes->append($node->name);
            $node->name = join($scope->prefixes->arr, '__');
        } elseif (($node instanceof PHPParser_Node_Expr_Variable)) {
            try {
                $cfg = $this->scopes[-2]->vars[$node->name];
                $name = uu(snow_list(array($cfg->prefix, $node->name)));
                foreach ($cfg->nodes as $x) {
                    $x->var->name = $name;
                }
                unset($x);
                $node->name = $name;
                if ($this->func) {
                    $this->func->uses[] = new PHPParser_Node_Expr_Variable($name);
                }
            } catch (IndexError $e) {
                
            } catch (KeyError $f) {
                
            }
        } elseif (($node instanceof PHPParser_Node_Expr_FuncCall) && \snow_eq(count($node->name->parts), 1)) {
            try {
                $node_name = $node->name->parts[0];
                $node->name->parts[0] = '$' . $this->scopes[-1]->fns[$node_name]->name;
            } catch (IndexError $e) {
                
            } catch (KeyError $f) {
                
            }
        } else {
            
        }
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
