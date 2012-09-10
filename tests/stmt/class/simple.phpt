--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

php_to_php('<?php
class X {
    function __construct() {
        $this->a = 42;
    }
}

'); die;
snowscript_to_php("

class A(array a, MyClass b, c=42, d)
    a = get_this(a)
    b = SuperMand(is_good(), 42, Oops())
    FOO_BAR = 1
    d = a*2

    fn x()
        <- 200

", 1);
die;

snowscript_to_php("
class A extends B implements C, D
    const A = 'B', C = 'D'
    public a = 'b', c = 'd'
    protected e
    private f

    public fn a
        pass
    public static fn b
        pass
    public final fn c() 
        pass
    protected fn d()
        pass
    private fn e() 
        pass
", 0);
--EXPECT--
<?php
class A extends B implements C, D
{
    const A = 'B', C = 'D';
    public $a = 'b', $c = 'd';
    protected $e;
    private $f;
    public function a()
    {
        
    }
    public static function b()
    {
        
    }
    public final function c()
    {
        
    }
    protected function d()
    {
        
    }
    private function e()
    {
        
    }
}
