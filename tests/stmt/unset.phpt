--TEST--
Unset
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
unset(a)
unset(b, c)
', 0);
--EXPECT--
<?php
unset($a);
unset($b, $c);