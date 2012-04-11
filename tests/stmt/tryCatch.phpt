--TEST--
Try/catch
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
try {
} catch (A b) {
} catch (B c) {
}
', 0);
--EXPECT--
<?php
try {
} catch (A $b) {
} catch (B $c) {
}