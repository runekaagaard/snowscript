--TEST--
Arguments
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
f()
f(a)
f(a, b)
f(&a)
', 0);
--EXPECT--
<?php
f();
f($a);
f($a, $b);
f(&$a);