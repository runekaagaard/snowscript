--TEST--
Comparison operators
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a < b
a <= b
a > b
a >= b
a == b
a === b
a != b
a !== b
a instanceof B
a instanceof b
', 0);
--EXPECT--
<?php
$a < $b;
$a <= $b;
$a > $b;
$a >= $b;
$a == $b;
$a === $b;
$a != $b;
$a !== $b;
$a instanceof B;
$a instanceof $b;