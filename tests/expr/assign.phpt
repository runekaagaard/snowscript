--TEST--
Testing Hello World example.
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
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
[a, b, c] = d
[a, [null, c], d] = e
[a, null, c] = c
[a, [foo['bar'], foo.bar, [d(), E]], f] = g
SNOW
);
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
list($a, $b, $c) = $d;
list($a, list(, $c), $d) = $e;
list($a, , $c) = $c;
list($a, list($foo['bar'], $foo->bar, list(d(), E)), $f) = $g;