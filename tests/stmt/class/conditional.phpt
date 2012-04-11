--TEST--
Conditional class definition
--FILE--
<?php
if (true) {
    class A {}
}
--EXPECT--
<?php
if (true) {
    class A {}
}