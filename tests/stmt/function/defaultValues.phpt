--TEST--
Default values (static scalar tests)
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php(<<<EOT
fn a(b = null, 
     c = 'foo',
     d = A::B,     
     e = +1,
     f = -1.0,
     g = [],
     h = ['foo', 'bar': 'baz'],
     i = ['foo'])
    pass
EOT
, 0);
--EXPECT--
<?php
function a($b = null, $c = 'foo', $d = A::B, $e = +1, $f = -1.0, $g = array(), $h = array('foo', 'bar' => 'baz'), $i = array('foo'))
{
    
}