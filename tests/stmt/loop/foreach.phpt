--TEST--
Foreach loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
for b in a
    pass
for &b in a
    pass
for b,c in a
    pass
for b,&c in a
    pass
for b in []
    pass
', 1);
--EXPECT--
<?php
foreach ($a as $b)  {}
foreach ($a as &$b) {}
foreach ($a as $b => $c) {}
foreach ($a as $b => &$c) {}
foreach (array() as $b) {}
foreach ($a as $b):
endforeach;