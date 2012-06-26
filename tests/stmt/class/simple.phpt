--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("
class A
    b = get_this(And(that)) + 42
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
