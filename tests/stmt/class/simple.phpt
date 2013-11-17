--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("

class A 
    extends B
    implements C
    
    !FOO_BAR = 1

    implements D

    y = a
    a = my_func(a)
    x = MyClass(my_other_func2(), y, MyOtherClass())
    d = x*2 * a * b
    ds = []

    fn __construct(array a, MyClass b, c=42, ds)
        for d in ds
            .ds[] = do_stuff_to(d)
        .x.y.z.v.f = 2000
        xs.superman

    fn x()
        <- 200

", 1);
--EXPECT--
<?php
class A extends B implements C, D
{
    const FOO_BAR = 1;
    public $y = $a;
    public $a = my_func($a);
    public $x = new MyClass(my_other_func2(), $y, new MyOtherClass());
    public $d = (($x * 2) * $a) * $b;
    public $ds = array();
    
    public function __construct(array $a, MyClass $b, $c = 42, $ds)
    {
        foreach ($ds as $d) {
            $this->ds[] = do_stuff_to($d);
        }
        unset($d);
        $this->x->y->z->v->f = 2000;
        $xs->superman;
    }
    
    public function x()
    {
        return 200;
    }

}