--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'


from foo.bar import my_fn, MyThing, MY_VAR
my_fn()
MyThing
MY_VAR
fn x
    MY_VAR
    MyThing

SNOW
, 0);
--EXPECT--
global $foo__bar__my_fn, $foo__bar__MyThing, $foo__bar__MY_VAR;
$foo__bar__my_fn();
$foo__bar__MyThing;
$foo__bar__MY_VAR;