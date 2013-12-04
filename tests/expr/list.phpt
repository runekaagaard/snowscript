--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

execute_snow("

a = []
a->count()->var_dump()
a.append('i')->count()->var_dump()
a.extend([1,2,42]).get(3)->var_dump()
a.pop()->var_dump()
a->count()->var_dump()

");
--EXPECT--
int(0)
int(1)
int(42)
int(42)
int(3)