--TEST--
Casts
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
(array)   a
(bool)    a
(boolean) a
(real)    a
(double)  a
(float)   a
(int)     a
(integer) a
(object)  a
(string)  a
(unset)   a
', 0);
--EXPECT--
<?php
(array)   $a;
(bool)    $a;
(boolean) $a;
(real)    $a;
(double)  $a;
(float)   $a;
(int)     $a;
(integer) $a;
(object)  $a;
(string)  $a;
(unset)   $a;