--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("
class A
    public
        private
            protected
                static
                    final
                        abstract
                            b = [1,2,3]
", 1);
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