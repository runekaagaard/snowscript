--TEST--
Return and pass by ref
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php('
fn a(&b)
	pass
fn &c(d)
	pass
', 0);
--EXPECT--
<?php
function a(&$b)
{
    
}
function &c($d)
{
    
}