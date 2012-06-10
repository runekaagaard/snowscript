--TEST--
Switch
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
fn x
    switch a
        0
            b
            c
        2,3
            d
            e
    switch f
        4,5,9,10
            g
            h
        6,7
            i
            j

switch a
    0,1
        b
        c
    default
        d
        e
switch f
    4,5
        g
        h
    6,7,default
        i
        j
', 0);
--EXPECT--
<?php
function x()
{
    switch ($a) {
        case 0:
            $b;
            $c;
            break;
        case 2:
        case 3:
            $d;
            $e;
            break;
    }
    switch ($f) {
        case 4:
        case 5:
        case 9:
        case 10:
            $g;
            $h;
            break;
        case 6:
        case 7:
            $i;
            $j;
            break;
    }
}
switch ($a) {
    case 0:
    case 1:
        $b;
        $c;
        break;
    default:
        $d;
        $e;
        break;
}
switch ($f) {
    case 4:
    case 5:
        $g;
        $h;
        break;
    case 6:
    case 7:
    default:
        $i;
        $j;
        break;
}
