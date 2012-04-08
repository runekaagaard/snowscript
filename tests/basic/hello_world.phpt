--TEST--
Testing Hello World example.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

$code = 'echo "Hello, Snowscript World!"
';

$lexer = new Snowscript_Lexer($code);
$parser = new PHPParser_Parser;
$prettyPrinter = new PHPParser_PrettyPrinter_Zend;

$stmts = $parser->parse($lexer);
echo "<?php\n" . $prettyPrinter->prettyPrint($stmts) . "\n";
--EXPECT--
<?php
echo "Hello, Snowscript World!";