--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
a = 42
b = 44

fn c
    echo d

SNOW
, 0);
--EXPECT--
<?php
