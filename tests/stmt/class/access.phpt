--TEST--
Class declaration
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php("

class A 
    fn __construct()
        ..foo
        .foo
        parent..bos()

    fn baz
        ..baz
        .baz
        parent..baz()

", 0);
--EXPECT--
<?php
class A
{
    
    public function __construct()
    {
        self::$foo;
        $this->foo;
        parent::bos();
    }
    
    public function baz()
    {
        self::$baz;
        $this->baz;
        parent::baz();
    }
}