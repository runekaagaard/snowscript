--TEST--
Casts
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
array(a)
bool(a)
float(a)
int(a)
object(a)
str(a)
', 0);
--EXPECT--
<?php
(array) $a;
(bool) $a;
(double) $a;
(int) $a;
(object) $a;
(string) $a;
