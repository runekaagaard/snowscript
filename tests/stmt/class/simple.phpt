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

    y = 50
    ds = []

    fn __construct(array a, MyClass b, c=42, ds)
        for d in ds
            .ds[] = do_stuff_to(d)
        .x.y.z.v.f = 2000
        xs.superman

    fn x()
        <- 200

", 0);
--EXPECT--
<?php
class A extends B implements C, D
{
    const FOO_BAR = 1;
    public $y = 50;
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