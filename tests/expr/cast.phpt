--TEST--
Casts
--FILE--
<?php
(array)   a;
(bool)    a;
(boolean) a;
(real)    a;
(double)  a;
(float)   a;
(int)     a;
(integer) a;
(object)  a;
(string)  a;
(unset)   a;
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