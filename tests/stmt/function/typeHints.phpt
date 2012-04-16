--TEST--
Type hints
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
fn a(b, array c, callable e, E f)
	<- 42
', 0);
--EXPECT--
<?php
function a($b, array $c, callable $e, E $f)
{
    return 42;
}