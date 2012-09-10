--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("

class A(array a, MyClass b, c=42, ds)
    FOO_BAR = 1

    y = a
    a = my_func(a)
    x = MyClass(my_other_func2(), y, MyOtherClass())
    d = x*2 * a * b
    ds = []

    fn __construct
        for d in ds
            .ds[] = do_stuff_to(d)
        .x.y.z.v.f = 2000
        xs.superman

    fn x()
        <- 200

", 0);

--EXPECT--
<?php
class A
{
    const FOO_BAR = 1;
    
    public function x()
    {
        return 200;
    }

    public function __construct(array $a, MyClass $b, $c = 42, $ds) {
        $this->y = $a;
        $this->a = my_func($a);
        $this->x = new MyClass(my_other_func2(), $this->y, new MyOtherClass());
        $this->d = (($this->x * 2) * $this->a) * $b;
        $this->ds = array();
        foreach ($ds as $d) {
            $this->ds[] = do_stuff_to($d);
        }
        unset($d);
        $this->x->y->z->v->f = 2000;
        $xs->superman;
    }

}