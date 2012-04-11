--TEST--
Final class
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
final class A {}
', 0);
--EXPECT--
<?php
final class A {}