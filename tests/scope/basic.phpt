--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
XX = 10
i = 0
fn a
    i = 1
    XX
    YY = 10
    a() + b() + c()
    fn b
        XX + YY
        ZZ = 10
        a() + b() + c()
        fn c
            XX + YY + ZZ
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
global $InjectMe__XX, $InjectMe__a__YY, $InjectMe__a_b__ZZ, $InjectMe__JJ;
$InjectMe__XX = 10;
$i = 0;
function a()
{
    global $InjectMe__XX;
    $i = 1;
    $InjectMe__XX;
    $InjectMe__a__YY = 10;
    (a() + b()) + c();
    function b()
    {
        global $InjectMe__XX, $InjectMe__a__YY;
        $InjectMe__XX + $InjectMe__a__YY;
        $InjectMe__a_b__ZZ = 10;
        (a() + b()) + c();
        function c()
        {
            global $InjectMe__XX, $InjectMe__a__YY, $InjectMe__a_b__ZZ;
            ($InjectMe__XX + $InjectMe__a__YY) + $InjectMe__a_b__ZZ;
            (a() + b()) + c();
        }
    }
    (a() + b()) + c();
}
(a() + b()) + c();
function d()
{
    
}
$InjectMe__JJ = 10;
echo $InjectMe__JJ;
$k = 10;
echo $k;