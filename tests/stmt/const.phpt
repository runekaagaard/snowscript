--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
const A = 0, B = 1.0, C = \'A\', D = E
', 0);
--EXPECT--
<?php
const A = 0, B = 1.0, C = 'A', D = E;