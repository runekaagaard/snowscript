--TEST--
Foreach loop
--FILE--
<?php
foreach (a as b)  {}
foreach (a as &b) {}
foreach (a as b => c) {}
foreach (a as b => &c) {}
foreach (array() as b) {}
foreach (a as b):
endforeach;
--EXPECT--
<?php
foreach ($a as $b)  {}
foreach ($a as &$b) {}
foreach ($a as $b => $c) {}
foreach ($a as $b => &$c) {}
foreach (array() as $b) {}
foreach ($a as $b):
endforeach;