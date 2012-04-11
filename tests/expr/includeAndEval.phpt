--TEST--
Include and eval
--FILE--
<?php
include 'A.php';
include_once 'A.php';
require 'A.php';
require_once 'A.php';
eval('A');
--EXPECT--
<?php
include 'A.php';
include_once 'A.php';
require 'A.php';
require_once 'A.php';
eval('A');