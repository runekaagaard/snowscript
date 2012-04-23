--TEST--
Inline HTML
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
a
%> 
<?php echo $yo ?>
<%
c
%> 
d
', 0);
--EXPECT--
<?php
$a;
?> 
<?php echo $yo ?>
<?php 
$c;
?> 
d

<?php