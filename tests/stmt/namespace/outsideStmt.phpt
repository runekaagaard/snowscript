--TEST--
Some statements may occur outside of namespaces
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
declare(A=\'B\')
namespace B {
}
__halt_compiler()
?>
Hi!
', 0);
--EXPECT--
<?php
declare(A='B');
namespace B {
}
__halt_compiler()
?>
Hi!