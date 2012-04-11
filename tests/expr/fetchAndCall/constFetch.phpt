--TEST--
Constant fetches
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
A
A::B
', 0);
--EXPECT--
<?php
A;
A::B;