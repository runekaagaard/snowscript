--TEST--
Comparison operators
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

// TODO: a isa b needs to use scope knowlegde.
snowscript_to_php('
a < b
a <= b
a > b
a >= b
a == b
a === b
a != b
a !== b
a isa B
a isa b
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