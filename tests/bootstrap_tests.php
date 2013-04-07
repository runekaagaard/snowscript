<?php

error_reporting(E_ALL|E_STRICT);
ini_set('display_errors', 1);

require dirname(__FILE__) . '/../libs/PHP-Parser/lib/bootstrap.php';
require dirname(__FILE__) . '/../src/php/bootstrap.php';

function line($msg) {
	echo $msg . "\n";
}

class Scope extends PHPParser_NodeVisitorAbstract {
	public $assigns = array(array());
	public $fns = null;
	public $names = array(array());
	public $namespace = 'InjectMe';
	public $in_assign = false;

	function beforeTraverse(array $nodes) {
		array_unshift($nodes, new PHPParser_Node_Stmt_Global(array()));
		$this->fns = array($nodes[0]);
		return $nodes;
	}

	function afterTraverse(array $nodes) {
		if (empty($nodes[0]->vars)) array_shift($nodes);
		return $nodes;
	}

	function enterNode(PHPParser_Node $node) {
		$count = count($this->assigns);
		$cur = $count - 1;
		$prev = $count > 1 ? $count - 2 : false;
		$is_all_caps = function($x) { return $x === strtoupper($x); };

		if ($node instanceof PHPParser_Node_Expr_Assign 
			&& $is_all_caps($node->var->name)) 
		{
			$this->assigns[$cur][$node->var->name] []= $node;
		} elseif ($node instanceof PHPParser_Node_Stmt_Function 
		          || $node instanceof PHPParser_Node_Stmt_ClassMethod) 
		{
			$this->assigns []= $this->assigns[$cur];
			$this->names []= $this->names[$cur];
			$this->fns []= $node;
			array_unshift($node->stmts, 
				          new PHPParser_Node_Stmt_Global(array()));
		} elseif ($node instanceof PHPParser_Node_Expr_Variable
	              && (
	              ($prev !== false && isset($this->assigns[$prev][$node->name]))
		          ||
		          ($prev === false && isset($this->assigns[$cur][$node->name])
		          ))) 
		{
			if (empty($this->names[$cur][$node->name])) {
				$name = $this->global_var_name($node);
				$this->names[$cur][$node->name] = $name;
			} else {
				$name = $this->names[$cur][$node->name];
			}
			foreach ($this->assigns[$cur][$node->name] as $assign_node) {
				$assign_node->var->name = $name;	
			}
			$node->name = $name;
			$this->fns[$cur]->stmts[0]->vars[$name] = new PHPParser_Node_Expr_Variable($node->name);
			$this->fns[0]->vars[$name] = new PHPParser_Node_Expr_Variable($name);
		}
        return $node;
    }

    function fn_name($node) {
    	$read_name = function($v) { return $v->name; };
    	$names = array_map($read_name, array_slice($this->fns, 1));
    	if ($names) return implode('_', $names) . '__' . $node->name;
    	else return $node->name;
    }

    function global_var_name($node) {
    	$read_name = function($v) { return $v->name; };
    	$names = array_map($read_name, array_slice($this->fns, 1, -1));
    	if ($names) $names_str = implode('_', $names) . '__';
    	else $names_str = '';
    	return $this->namespace . '__' . $names_str . $node->name;
    }

	public function leaveNode(PHPParser_Node $node) {
		if ($node instanceof PHPParser_Node_Expr_Assign) {
		} elseif ($node instanceof PHPParser_Node_Stmt_Function 
		|| $node instanceof PHPParser_Node_Stmt_ClassMethod) {
			if (empty($node->stmts[0]->vars)) array_shift($node->stmts);
			array_pop($this->fns);
			array_pop($this->assigns);
			array_pop($this->names);
		} else
        return $node;
    }
}

function snowscript_to_php($code, $debug=false, $return=false) {
	$lexer = new Snowscript_Lexer($code . "\n");
	if ($debug) debug_lexer($lexer);
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	$traverser = new PHPParser_NodeTraverser;
	$traverser->addVisitor(new Scope);
	$stmts = $traverser->traverse($stmts);
	$nodeDumper = new PHPParser_NodeDumper;
	if ($debug) {
		$nodeDumper = new PHPParser_NodeDumper;
		echo $nodeDumper->dump($stmts) . "\n";
	}
	$php = $prettyPrinter->prettyPrint($stmts) . "\n";
	if ($return)
	    return $php;
	else
	    print "<?php\n" . $php;
}

function php_to_php($code, $debug=false) {
	$lexer = new PHPParser_Lexer($code . "\n");
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	$nodeDumper = new PHPParser_NodeDumper;
	echo $nodeDumper->dump($stmts);
}

function debug_lexer($lexer) {
	$fmt = str_repeat('%-30s', 4);
	line(sprintf($fmt, "In type", "In value", "Out type", "Out value"));
	line(sprintf($fmt, "-------", "--------", "--------", "---------"));
	foreach ($lexer->debug as $row) {
		line(sprintf($fmt,
			         $row['in_type'], str_replace("\n", '\n', "'" . $row['in_value'] . "'"),
			         $row['out_type'], "'" . $row['out_value'] . "'"));
	}
}
function prettyprint_tokens($tokens) {
	foreach ($tokens as $t)
		if (is_array($t))
			echo token_name($t[0]) === 'T_WHITESPACE'
				? ""
				: token_name($t[0]) . ': ' . trim($t[1]) . "\n";
		else
			echo $t . "\n";
}

function pp_php($code) {
	prettyprint_tokens(token_get_all("<?php " . $code));
}
