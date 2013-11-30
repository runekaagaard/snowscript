--TEST--
Compiling files in a project.
--FILE--
<?php
$ROOT = dirname(realpath(__FILE__));
echo(`$ROOT/../../bin/snow-compile $ROOT/project/a_path/afile.snow`);
echo "----\n";
echo(`$ROOT/../../bin/snow-compile $ROOT/project/test.snow`);
--EXPECT--
<?php
global $project__a_path__afile__MY_VAR;
$project__a_path__afile__MY_VAR = 42;
function foo()
{
    global $project__a_path__afile__MY_VAR;
    $project__a_path__afile__MY_VAR;
}
----
<?php
global $project__test__MY_VAR;
$project__test__MY_VAR = 42;
function foo()
{
    global $project__test__MY_VAR;
    $project__test__MY_VAR;
}