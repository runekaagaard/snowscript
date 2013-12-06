--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
XX = 10
i = 0
fn a
    a() + b() + c()
    fn b
        a() + b() + c()
        fn c
            a() + b() + c()

    a() + b() + c()

a() + b() + c()

fn d
    pass

JJ = 10
echo JJ
k = 10
echo k
SNOW
, 0);
--EXPECT--
<?php
global $Anonymous__XX, $Anonymous__a__YY, $Anonymous__a_b__ZZ, $Anonymous__JJ;
$Anonymous__XX = 10;
$i = 0;
function a()
{
    global $Anonymous__XX;
    $i = 1;
    $Anonymous__XX;
    $Anonymous__a__YY = 10;
    (a() + b()) + c();
    function b()
    {
        global $Anonymous__XX, $Anonymous__a__YY;
        $Anonymous__XX + $Anonymous__a__YY;
        $Anonymous__a_b__ZZ = 10;
        (a() + b()) + c();
        function c()
        {
            global $Anonymous__XX, $Anonymous__a__YY, $Anonymous__a_b__ZZ;
            ($Anonymous__XX + $Anonymous__a__YY) + $Anonymous__a_b__ZZ;
            (a() + b()) + c();
        }
    }
    (a() + b()) + c();
}
(a() + b()) + c();
function d()
{
    
}
$Anonymous__JJ = 10;
echo $Anonymous__JJ;
$k = 10;
echo $k;