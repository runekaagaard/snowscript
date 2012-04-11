--TEST--
Conditional function definition
--FILE--
<?php
if (true) {
    function A() {}
}
--EXPECT--
<?php
if (true) {
    function A() {}
}