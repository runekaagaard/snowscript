--TEST--
Do loop
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
do {
} while (a)
', 0);
--EXPECT--
<?php
do {
} while ($a);