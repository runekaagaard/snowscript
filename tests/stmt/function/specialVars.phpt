--TEST--
Special function variables
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

// TODO:
//{
//    global $a, $b, $c;
//    static $c, $d = 'e';
//}
snowscript_to_php("
fn a()
    global a, b, c
    static c, d = 'e'
", 0);
--EXPECT--
<?php
function a()
{
    global $a, $b, $c;
    static $c, $d = 'e';
}