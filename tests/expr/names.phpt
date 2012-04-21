--TEST--
Variables
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
AClass()
a.b.c
a.b.CAP
A::prop
a::func()
A::A_KONST
avar
afunccall()
A_KONST
foo(&a, &b)
fn foo(MyClass bar, array fox, zup)
	pass	
", 0);
--EXPECT--
<?php
new AClass();
$a->b->c;
$a->b->CAP;
A::$prop;
$a::func();
A::A_KONST;
$avar;
afunccall();
A_KONST;
foo(&$a, &$b);
function foo(MyClass $bar, array $fox, $zup)
{
    
}