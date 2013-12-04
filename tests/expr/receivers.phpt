--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
xx->func(32)
42->doit() - 41
hi->ok()
array_slice(guy, start, guy->count()-stop)
", 0);
--EXPECT--
<?php
func($xx, 32);
doit(42) - 41;
ok($hi);
array_slice($guy, $start, count($guy) - $stop);