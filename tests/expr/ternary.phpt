--TEST--
Ternary operator
--FILE--
<?php
a ? b : c;
a ?: c;
a ? b : c ? d : e;
a ? b : (c ? d : e);
--EXPECT--
<?php
$a ? $b : $c;
$a ?: $c;
$a ? $b : $c ? $d : $e;
$a ? $b : ($c ? $d : $e);