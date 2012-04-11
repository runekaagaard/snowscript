--TEST--
__halt_compiler
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a
__halt_compiler()
?>
Hallo World!
', 0);
--EXPECT--
<?php
$a;
__halt_compiler()
?>
Hallo World!