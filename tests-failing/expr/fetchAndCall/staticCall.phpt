--TEST--
Static calls
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("
A..b()
A..b['c']()
A..b['c']['d']()
", 0);
--EXPECT--
<?php
A::b();
A::$b['c']();
A::$b['c']['d']();