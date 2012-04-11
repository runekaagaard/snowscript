--TEST--
Inline HTML
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a
?>
B
c
?>
d
', 0);
--EXPECT--
<?php
$a;
?>
B
<?php
$c;
?>
<?php
$d;