--TEST--
Arguments
--FILE--
<?php
f();
f(a);
f(a, b);
f(&a);
--EXPECT--
<?php
f();
f($a);
f($a, $b);
f(&$a);