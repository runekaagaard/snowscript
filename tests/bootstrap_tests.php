<?php

require dirname(__FILE__) . '/../libs/PHP-Parser/lib/bootstrap.php';
require dirname(__FILE__) . '/../src/php/bootstrap.php';

function snowscript_to_php($code) {
	$lexer = new Snowscript_Lexer($code . "\n");
	$parser = new PHPParser_Parser;
	$prettyPrinter = new PHPParser_PrettyPrinter_Zend;
	$stmts = $parser->parse($lexer);
	echo "<?php\n" . $prettyPrinter->prettyPrint($stmts) . "\n";
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