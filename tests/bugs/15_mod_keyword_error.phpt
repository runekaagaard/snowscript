--TEST--
If/Elseif/Else
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php("
class A
    extends StandardController

    fn index
        .load.model('HelloModel', 'hello')
        .hello.say_hi('john')
        echo 5 mod 2
", 0);
--EXPECT--
<?php
class A extends StandardController
{
    
    public function index()
    {
        $this->load->model('HelloModel', 'hello');
        $this->hello->say_hi('john');
        echo 5 % 2;
    }



}