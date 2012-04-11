--TEST--
Math operators
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
bnot a
+a
-a
a band b
a bor b
a bxor b
a % b
a / b
a - b
a mod b
a * b
a + b
a bleft b
a bright b
a * b * c
a * (b * c)
a + b * c
(a + b) * c
', 0);
--EXPECT--
<?php
~$a;
+$a;
-$a;
$a & $b;
$a | $b;
$a ^ $b;
$a . $b;
$a / $b;
$a - $b;
$a % $b;
$a * $b;
$a + $b;
$a << $b;
$a >> $b;
($a * $b) * $c;
$a * ($b * $c);
$a + $b * $c;
($a + $b) * $c;