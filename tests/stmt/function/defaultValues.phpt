--TEST--
Default values (static scalar tests)
--FILE--
<?php
function a(
    b = null,
    c = 'foo',
    d = A::B,
    f = +1,
    g = -1.0,
    h = array(),
    i = [],
    j = ['foo'],
    k = ['foo', 'bar' => 'baz']
) {}
--EXPECT--
<?php
function a(
    $b = null,
    $c = 'foo',
    $d = A::B,
    $f = +1,
    $g = -1.0,
    $h = array(),
    $i = [],
    $j = ['foo'],
    $k = ['foo', 'bar' => 'baz']
) {}