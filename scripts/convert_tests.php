<?php

$files = array_filter(explode("\n", `find . -iname *.test`));
foreach ($files as $file) {
	list($title, $code) = explode('-----', file_get_contents($file));
	$title = trim($title);
	$code = trim($code);
	$code = preg_replace('#//.*#', "", $code);
	$code = preg_replace("#\n{2,}#", "\n", $code);
	$snow_code = str_replace("$", "", $code);
	$test =
"--TEST--
$title
--FILE--
$snow_code
--EXPECT--
$code";
	$new_filename = str_replace(".test", ".phpt", $file);
	file_put_contents($new_filename, $test);
}