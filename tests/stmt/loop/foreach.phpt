--TEST--
Foreach loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
fn x
    for b in 1 downto 10 step 2
        echo b
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