--TEST--
Control flow statements
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
break
break 2
continue
continue 2
return
return a
throw e
label:
goto label
', 0);
--EXPECT--
<?php
break;
break 2;
continue;
continue 2;
return;
return $a;
throw $e;
label:
goto label;