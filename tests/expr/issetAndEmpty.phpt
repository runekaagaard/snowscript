--TEST--
isset() and empty()
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
isset(a)
isset(a, b, c)
empty(a)
', 0);
--EXPECT--
<?php
isset($a);
isset($a, $b, $c);
empty($a);