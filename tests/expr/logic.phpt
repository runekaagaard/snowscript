--TEST--
Logical operators
--FILE--
<?php
a && b;
a || b;
!a;
!!a;
a and b;
a or b;
a xor b;
a && b || c && d;
a && (b || c) && d;
a = b || c;
a = b or c;
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