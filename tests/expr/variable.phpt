--TEST--
Variable syntaxes
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a
{\'a\'}
{foo()}
a
a
a[\'b\']
', 0);
--EXPECT--
<?php
$a;
${'a'};
${foo()};
$$a;
$$$a;
$$a['b'];