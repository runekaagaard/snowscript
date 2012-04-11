--TEST--
Simple array access
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a[\'b\']
a[\'b\'][\'c\']
a[] = b
a{\'b\'}
{a}[\'b\']
', 0);
--EXPECT--
<?php
$a['b'];
$a['b']['c'];
$a[] = $b;
$a{'b'};
${$a}['b'];