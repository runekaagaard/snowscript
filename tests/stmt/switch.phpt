--TEST--
Switch
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
switch (a) {
    case 0:
    case 1
    default:
}
switch (a):
endswitch
switch (a) {  }
switch (a):  endswitch
', 0);
--EXPECT--
<?php
switch ($a) {
    case 0:
    case 1;
    default:
}
switch ($a):
endswitch;
switch ($a) { ; }
switch ($a): ; endswitch;