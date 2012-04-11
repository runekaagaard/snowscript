--TEST--
Static calls
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
A::b()
A::{\'b\'}()
A::b()
A::b[\'c\']()
A::b[\'c\'][\'d\']()
A::b()[\'c\']
static::b()
a::b()
{\'a\'}::b()
a[\'b\']::c()
', 0);
--EXPECT--
<?php
A::b();
A::{'b'}();
A::$b();
A::$b['c']();
A::$b['c']['d']();
A::b()['c'];
static::b();
$a::b();
${'a'}::b();
$a['b']::c();