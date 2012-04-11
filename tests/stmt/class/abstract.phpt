--TEST--
Abstract class
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
abstract class A {
    public function a() {}
    abstract public function b()
}
', 0);
--EXPECT--
<?php
abstract class A {
    public function a() {}
    abstract public function b();
}