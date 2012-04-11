--TEST--
Try/catch
--FILE--
<?php
try {
} catch (A b) {
} catch (B c) {
}
--EXPECT--
<?php
try {
} catch (A $b) {
} catch (B $c) {
}