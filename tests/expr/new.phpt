--TEST--
New
--FILE--
<?php
new A;
new A(b);
new a();
new a['b']();
new A::b();
new a->b();
new a->b->c();
new a->b['c']();
new a->b{'c'}();
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