--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
a = 42
b = 32
c = 52
echo a
fn b()
	echo a

SNOW
, 0);
--EXPECT--
<?php
