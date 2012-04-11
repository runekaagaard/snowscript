--TEST--
Switch
--FILE--
<?php
switch (a) {
    case 0:
    case 1;
    default:
}
switch (a):
endswitch;
switch (a) { ; }
switch (a): ; endswitch;
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