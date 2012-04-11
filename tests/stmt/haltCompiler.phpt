--TEST--
__halt_compiler
--FILE--
<?php
a;
__halt_compiler()
?>
Hallo World!
--EXPECT--
<?php
$a;
__halt_compiler()
?>
Hallo World!