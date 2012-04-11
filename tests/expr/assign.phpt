--TEST--
Assignments
--FILE--
<?php
a = b;
a &= b;
a |= b;
a ^= b;
a .= b;
a /= b;
a -= b;
a %= b;
a *= b;
a += b;
a <<= b;
a >>= b;
a =& b;
a =& new B;
list(a) = b;
list(a, , b) = c;
list(a, list(, c), d) = e;
++a;
a++;
--a;
a--;
--EXPECT--
<?php
$a = $b;
$a &= $b;
$a |= $b;
$a ^= $b;
$a .= $b;
$a /= $b;
$a -= $b;
$a %= $b;
$a *= $b;
$a += $b;
$a <<= $b;
$a >>= $b;
$a =& $b;
$a =& new B;
list($a) = $b;
list($a, , $b) = $c;
list($a, list(, $c), $d) = $e;
++$a;
$a++;
--$a;
$a--;