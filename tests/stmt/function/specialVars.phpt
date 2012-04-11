--TEST--
Special function variables
--FILE--
<?php
function a() {
    global a, {'b'}, c;
    static c, d = 'e';
}
--EXPECT--
<?php
function a() {
    global $a, ${'b'}, $$c;
    static $c, $d = 'e';
}