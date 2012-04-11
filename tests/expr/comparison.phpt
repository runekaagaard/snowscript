--TEST--
Comparison operators
--FILE--
<?php
a < b;
a <= b;
a > b;
a >= b;
a == b;
a === b;
a != b;
a !== b;
a instanceof B;
a instanceof b;
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