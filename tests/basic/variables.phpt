--TEST--
Testing variables
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<_
a + b
_
);
--EXPECT--
<?php
$a + $b;