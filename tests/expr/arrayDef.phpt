--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
[]
[1, 2, 3]
[foo, BAR, !BAZ, Boo(), BOS]
", 0);
--EXPECT--
<?php
array();
array(1, 2, 3);
array($foo, $BAR, BAZ, new Boo(), $BOS);