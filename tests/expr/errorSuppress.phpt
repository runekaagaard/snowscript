--TEST--
Error suppression
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
@a
', 0);
--EXPECT--
<?php
@$a;