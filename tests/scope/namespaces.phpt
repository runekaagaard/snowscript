--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'

from foo.bar import my_fn, MyThing, MY_VAR
my_fn()
ABC = 10
SNOW
, 0);
--EXPECT--
<?php
global $foo__bar__my_fn, $foo__bar__MyThing, $foo__bar__MY_VAR;
$foo__bar__my_fn();
$foo__bar__MyThing;
$foo__bar__MY_VAR;
function x()
{
    global $foo__bar__my_fn, $foo__bar__MY_VAR, $foo__bar__MyThing;
    $foo__bar__my_fn();
    $foo__bar__MY_VAR;
    $foo__bar__MyThing;
}