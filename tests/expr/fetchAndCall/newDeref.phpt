--TEST--
New expression dereferencing
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
(new A)->b
(new A)->b()
(new A)[\'b\']
(new A)[\'b\'][\'c\']
', 0);
--EXPECT--
<?php
(new A)->b;
(new A)->b();
(new A)['b'];
(new A)['b']['c'];