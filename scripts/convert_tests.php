<?php

$files = array_filter(explode("\n", `find . -iname *.test`));
foreach ($files as $file) {
	list($title, $code) = explode('-----', file_get_contents($file));
	$title = trim($title);
	$code = trim($code);
	$code = preg_replace('#//.*#', "", $code);
	$code = preg_replace("#\n{2,}#", "\n", $code);
	$snow_code = str_replace("$", "", $code);
	$snow_code = str_replace(";", "", $snow_code);
	$snow_code = str_replace("'", "\'", $snow_code);
	$snow_code = preg_replace("#<\?php.*\n#", "", $snow_code);
	$test =
"--TEST--
$title
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php('
$snow_code
', 0);
--EXPECT--
$code";
	$new_filename = str_replace(".test", ".phpt", $file);
	file_put_contents($new_filename, $test);

}