--TEST--
Foreach loop
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
foreach (a as b)  {}
foreach (a as &b) {}
foreach (a as b => c) {}
foreach (a as b => &c) {}
foreach (array() as b) {}
foreach (a as b):
endforeach
', 0);
--EXPECT--
<?php
foreach ($a as $b)  {}
foreach ($a as &$b) {}
foreach ($a as $b => $c) {}
foreach ($a as $b => &$c) {}
foreach (array() as $b) {}
foreach ($a as $b):
endforeach;