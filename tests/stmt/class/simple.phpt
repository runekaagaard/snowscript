--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("

class A
    extends B
    implements C, D

    protected foo = 1922222
    final bar = 32
    !MY_CONST = 'HI'
    private baz = [1,2,3,4]
    dapub = 'uber'

    fn __construct(array a, MyClass b, c=42, ds)
        for d in ds
            .ds[] = do_stuff_to(d)
        .x.y.z.v.f = 2000
        xs.superman

    fn x()
        <- 200

    private static fn why()
        <- 'Dont know'

", 0);
--EXPECT--
<?php
class A extends B implements C, D
{
    protected $foo = 1922222;
    final $bar = 32;
    const MY_CONST = 'HI';
    private $baz = array(1, 2, 3, 4);
    public $dapub = 'uber';
    
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
    
    private static function why()
    {
        return 'Dont know';
    }
}