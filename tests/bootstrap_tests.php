<?php

error_reporting(E_ALL|E_STRICT);
ini_set('display_errors', 1);

require dirname(__FILE__) . '/../libs/PHP-Parser/lib/bootstrap.php';
require dirname(__FILE__) . '/../src/php/bootstrap.php';

function line($msg) {
	echo $msg . "\n";
}

function php_to_php($code, $debug=false) {
	$lexer = new PHPParser_Lexer($code . "\n");
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	$nodeDumper = new PHPParser_NodeDumper;
	echo $nodeDumper->dump($stmts);
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
