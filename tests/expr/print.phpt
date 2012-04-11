--TEST--
Print
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
print a
', 0);
--EXPECT--
<?php
print $a;