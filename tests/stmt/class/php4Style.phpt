--TEST--
PHP 4 style declarations
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
class A {
    var foo
    function bar() {}
}
', 0);
--EXPECT--
<?php
class A {
    var $foo;
    function bar() {}
}