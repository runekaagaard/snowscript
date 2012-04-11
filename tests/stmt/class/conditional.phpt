--TEST--
Conditional class definition
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
if (true) {
    class A {}
}
', 0);
--EXPECT--
<?php
if (true) {
    class A {}
}