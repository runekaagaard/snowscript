--TEST--
Interface
--FILE--
<?php
interface A extends C, D {
    public function a();
}
--EXPECT--
<?php
interface A extends C, D {
    public function a();
}