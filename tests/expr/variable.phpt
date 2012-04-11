--TEST--
Variable syntaxes
--FILE--
<?php
a;
{'a'};
{foo()};
a;
a;
a['b'];
--EXPECT--
<?php
$a;
${'a'};
${foo()};
$$a;
$$$a;
$$a['b'];