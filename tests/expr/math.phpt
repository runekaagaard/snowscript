--TEST--
Mathematical operators
--FILE--
<?php
~a;
+a;
-a;
a & b;
a | b;
a ^ b;
a . b;
a / b;
a - b;
a % b;
a * b;
a + b;
a << b;
a >> b;
a * b * c;
a * (b * c);
a + b * c;
(a + b) * c;
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
$a * $b * $c;
$a * ($b * $c);
$a + $b * $c;
($a + $b) * $c;