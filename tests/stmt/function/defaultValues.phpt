--TEST--
Default values (static scalar tests)
--FILE--
<?php
require dirname(__FILE__) . '/../../bootstrap_tests.php';

snowscript_to_php(<<<EOT
fn a(b = "null", 
     c = 'foo',
     d = A::B,
     # TODO: e = A::b,
     f = +1,
     g = -1.0,
     i = [],
     # TODO: j = ['foo', 'bar': 'baz'],
     k = ['foo']
     )
    pass
EOT
, 0);
--EXPECT--
<?php
function a($b = 'null', $c = 'foo', $d = A::B, $f = +1, $g = -1.0, $i = array(), $k = array('foo'))
{
    
}