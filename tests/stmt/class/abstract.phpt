--TEST--
Abstract class
--FILE--
<?php
abstract class A {
    public function a() {}
    abstract public function b();
}
--EXPECT--
<?php
abstract class A {
    public function a() {}
    abstract public function b();
}