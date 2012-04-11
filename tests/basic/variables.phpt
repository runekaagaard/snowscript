--TEST--
Variables
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
a + b
A + B
a()
");
--EXPECT--
<?php
$a + $b;
A + B;
a();