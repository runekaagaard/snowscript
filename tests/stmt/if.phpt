--TEST--
If/Elseif/Else
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
if      (a) {}
elseif  (b) {}
elseif  (c) {}
else         {}
if (a) {} 
if      (a):
elseif  (b):
elseif  (c):
else        :
endif
if (a): endif 
', 0);
--EXPECT--
<?php
if      ($a) {}
elseif  ($b) {}
elseif  ($c) {}
else         {}
if ($a) {} 
if      ($a):
elseif  ($b):
elseif  ($c):
else        :
endif;
if ($a): endif; 