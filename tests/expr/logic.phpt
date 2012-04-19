--TEST--
Logical operators
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a and b
a _and_ b
a or b
a _or_ b
not a
not not not a
a xor b
a and b or c and d
a and (b or c) and d
a = b or c
a = b _or_ c
', 0);
--EXPECT--
<?php
$a && $b;
$a and $b;
$a || $b;
$a or $b;
!$a;
!(!(!$a));
$a xor $b;
$a && $b || $c && $d;
($a && ($b || $c)) && $d;
$a = $b || $c;
$a = $b or $c;