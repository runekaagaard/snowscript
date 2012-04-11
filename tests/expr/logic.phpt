--TEST--
Logical operators
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a && b
a || b
!a
!!a
a and b
a or b
a xor b
a && b || c && d
a && (b || c) && d
a = b || c
a = b or c
', 0);
--EXPECT--
<?php
$a && $b;
$a || $b;
!$a;
!!$a;
$a and $b;
$a or $b;
$a xor $b;
$a && $b || $c && $d;
$a && ($b || $c) && $d;
$a = $b || $c;
$a = $b or $c;