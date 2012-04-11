--TEST--
Unset
--FILE--
<?php
unset(a);
unset(b, c);
--EXPECT--
<?php
unset($a);
unset($b, $c);