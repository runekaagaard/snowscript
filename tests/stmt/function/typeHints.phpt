--TEST--
Type hints
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
function a(b, array c, callable d, E f) {}
', 0);
--EXPECT--
<?php
function a($b, array $c, callable $d, E $f) {}