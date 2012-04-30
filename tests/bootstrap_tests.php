<?php

error_reporting(E_ALL|E_STRICT);
ini_set('display_errors', 1);

require dirname(__FILE__) . '/../libs/PHP-Parser/lib/bootstrap.php';
require dirname(__FILE__) . '/../src/php/bootstrap.php';

function line($msg) {
	echo $msg . "\n";
}

function snowscript_to_php($code, $debug=false) {
	$lexer = new Snowscript_Lexer($code . "\n");
	if ($debug) debug_lexer($lexer);
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	echo "<?php\n" . $prettyPrinter->prettyPrint($stmts) . "\n";
}

function php_to_php($code, $debug=false) {
	$lexer = new PHPParser_Lexer($code . "\n");
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	if ($debug) var_dump($stmts);
	echo $prettyPrinter->prettyPrint($stmts) . "\n";
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
