--TEST--
Simple array access
--FILE--
<?php
a['b'];
a['b']['c'];
a[] = b;
a{'b'};
{a}['b'];
--EXPECT--
<?php
$a['b'];
$a['b']['c'];
$a[] = $b;
$a{'b'};
${$a}['b'];