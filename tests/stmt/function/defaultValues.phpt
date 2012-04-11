--TEST--
Default values (static scalar tests)
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
function a(
    b = null,
    c = \'foo\',
    d = A::B,
    f = +1,
    g = -1.0,
    h = array(),
    i = [],
    j = [\'foo\'],
    k = [\'foo\', \'bar\' => \'baz\']
) {}
', 0);
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