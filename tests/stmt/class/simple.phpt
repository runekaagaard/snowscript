--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("

class A(array a, MyClass b, c=42, d)
    FOO_BAR = 1

    y = a
    a = my_func(a)
    x = MyClass(my_other_func2(), 42, MyOtherClass())
    d = x*2 * a * b

    fn x()
        <- 200

", 0);

--EXPECT--
<?php
class A
{
    const FOO_BAR = 1

    public function x()
    {
        return 200;
    }
function __construct(array $a, MyClass $b, $c = 42, $d) {
    $this->y = $a;
    $this->a = my_func($a);
    $this->x = new MyClass(my_other_func2(), 42, new MyOtherClass());
    $this->d = (($this->x * 2) * $this->a) * $b;
}
}