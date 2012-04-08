--TEST--
Testing math.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

$code = <<<CODE
10 + 20
10 - 20
10 * 20
10 / 20

CODE;

$lexer = new Snowscript_Lexer($code);
$parser = new PHPParser_Parser;
$prettyPrinter = new PHPParser_PrettyPrinter_Zend;

$stmts = $parser->parse($lexer);
echo "<?php\n" . $prettyPrinter->prettyPrint($stmts) . "\n";
--EXPECT--
<?php
10 + 20;
10 - 20;
10 * 20;
10 / 20;