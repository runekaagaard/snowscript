--TEST--
If/Elseif/Else
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
if (a)
	pass
elif (b)
	pass
elif (c)
	pass
', 1);
--EXPECT--
<?php
if      ($a) {}
elseif  ($b) {}
elseif  ($c) {}
else         {}
