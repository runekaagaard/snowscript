--TEST--
PHP 4 style declarations
--FILE--
<?php
class A {
    var foo;
    function bar() {}
}
--EXPECT--
<?php
class A {
    var $foo;
    function bar() {}
}