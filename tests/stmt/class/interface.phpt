--TEST--
Interface
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
interface A extends C, D
    fn a
    	pass
', 0);
--EXPECT--
<?php
interface A extends C, D
{
    
    public function a()
    {
        
    }
}