--TEST--
New
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
new A
new A(b)
new a()
new a[\'b\']()
new A::b()
new a->b()
new a->b->c()
new a->b[\'c\']()
new a->b{\'c\'}()
', 0);
--EXPECT--
<?php
new A;
new A($b);
new $a();
new $a['b']();
new A::$b();
new $a->b();
new $a->b->c();
new $a->b['c']();
new $a->b{'c'}();