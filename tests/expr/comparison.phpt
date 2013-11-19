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
a != b
a isa B
a isa b
', 0);
--EXPECT--
<?php
\snow_lt($a, $b);
\snow_lte($a, $b);
\snow_gt($a, $b);
\snow_gte($a, $b);
\snow_eq($a, $b);
\snow_neq($a, $b);
$a instanceof B;
$a instanceof $b;