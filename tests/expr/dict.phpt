--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

execute_snow(<<<'SNOW'

a = {}
a->count()->var_dump()
a.foo = "hello"
a.foo->var_dump()
{'x': 2} == {'x': 2} -> var_dump()

SNOW
);
--EXPECT--
int(0)
int(1)
int(42)
int(42)
int(3)