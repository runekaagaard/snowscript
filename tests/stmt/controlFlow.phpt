--TEST--
Control flow statements
--FILE--
<?php
break;
break 2;
continue;
continue 2;
return;
return a;
throw e;
label:
goto label;
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