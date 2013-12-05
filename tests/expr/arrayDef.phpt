--TEST--
Array definitions
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'
[]
[1, 2, 3]
[1, foo(), Foo(), !FOO, FOO, [1,2,3], {}]
{}
{
	'abc': 2,
	'nice': [{
		'why': 'not', 'key': {'x':[[[[[]]]]]},
	}],
}
scopes[-1].fns[node.name] = node
.scopes[-1].fns[node.name] = node
SNOW
, 0);
--EXPECT--
<?php
snow_list(array());
snow_list(array(1, 2, 3));
snow_list(array(1, foo(), new Foo(), FOO, $FOO, snow_list(array(1, 2, 3)), snow_dict(array())));
snow_dict(array());
snow_dict(array('abc' => 2, 'nice' => snow_list(array(snow_dict(array('why' => 'not', 'key' => snow_dict(array('x' => snow_list(array(snow_list(array(snow_list(array(snow_list(array(snow_list(array())))))))))))))))));
$this->scopes[-1]->fns[$node->name] = $node;