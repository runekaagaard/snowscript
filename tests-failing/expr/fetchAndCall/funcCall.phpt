--TEST--
Function calls
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("
a()
a['b']()
a.b['c']()
a()['b']
", 0);
--EXPECT--
<?php
a();
$a['b']();
$a->b['c']();
a()['b'];