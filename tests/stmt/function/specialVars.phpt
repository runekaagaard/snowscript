--TEST--
Special function variables
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
function a() {
    global a, {\'b\'}, c
    static c, d = \'e\'
}
', 0);
--EXPECT--
<?php
function a() {
    global $a, ${'b'}, $$c;
    static $c, $d = 'e';
}