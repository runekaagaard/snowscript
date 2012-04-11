--TEST--
Some statements may occur outside of namespaces
--FILE--
<?php
declare(A='B');
namespace B {
}
__halt_compiler()
?>
Hi!
--EXPECT--
<?php
declare(A='B');
namespace B {
}
__halt_compiler()
?>
Hi!