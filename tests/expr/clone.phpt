--TEST--
Clone
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
clone a
', 0);
--EXPECT--
<?php
clone $a;