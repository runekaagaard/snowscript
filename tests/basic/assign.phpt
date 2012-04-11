--TEST--
Testing Hello World example.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a = b
a band= b
a bor= b
a bxor= b
a %= b
a /= b
a -= b
a mod= b
a *= b
a += b
a bleft= b
a bright= b
a =& b
a =& new B
list(a) = b
list(a, , b) = c
list(a, list(, c), d) = e
++a
a++
--a
a--
');
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
$a = new B();
list($a) = $b;
list($a, , $b) = $c;
list($a, list(, $c), $d) = $e;
++$a;
$a++;
--$a;
$a--;