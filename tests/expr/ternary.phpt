--TEST--
Ternary operator
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a ? b : c
a ?: c
a ? b : c ? d : e
a ? b : (c ? d : e)
', 0);
--EXPECT--
<?php
$a ? $b : $c;
$a ?: $c;
$a ? $b : $c ? $d : $e;
$a ? $b : ($c ? $d : $e);