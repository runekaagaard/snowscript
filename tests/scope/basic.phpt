--TEST--
Global constants
--FILE--
<?php
require dirname(__FILE__) . '/../bootstrap_tests.php';

snowscript_to_php(<<<'SNOW'

a = 1
a
a = 1
fn b
	pass
a
a = 1
echo "......................."
x = 1
fn y
	y
	y = 1
fn decorate
	pass
y = y->decorate()
y()
echo "......................."
i = 10
fn j
	j
	j()
	i = 10
	i
	k = 10
	k
	l = 10
	l
	fn m
		j
		j()
		i
		i = 10
		k
		k = 10
echo "......................."
fn o
	fn p
		r = 5
		fn s
			r = 5
	fn q
		r = 10
		q()
	r = 10
	p()
	q()

SNOW
, 0);
--EXPECT--
<?php
$Anonymous__a = 1;
$Anonymous__a;
$Anonymous__a = 1;
$Anonymous__b = function ()
{
    
};
$Anonymous__a;
$Anonymous__a = 1;
echo ".......................";
$Anonymous__x = 1;
$Anonymous__y = function ()
{
    $Anonymous__y;
    $Anonymous__y = 1;
};
$Anonymous__decorate = function ()
{
    
};
$Anonymous__y = $Anonymous__decorate($y);
$Anonymous__y();
echo ".......................";
$Anonymous__i = 10;
$Anonymous__j = function ()
{
    $Anonymous__j;
    $Anonymous__j();
    $Anonymous__i = 10;
    $Anonymous__i;
    $Anonymous__j__k = 10;
    $Anonymous__j__k;
    $l = 10;
    $l;
    $m = function ()
    {
        $Anonymous__j;
        $Anonymous__j();
        $Anonymous__i;
        $Anonymous__i = 10;
        $Anonymous__j__k;
        $Anonymous__j__k = 10;
    };
};
echo ".......................";
$Anonymous__o = function ()
{
    $p = function ()
    {
        $Anonymous__o__p__r = 5;
        $s = function ()
        {
            $Anonymous__o__p__r = 5;
        };
    };
    $Anonymous__o__q = function ()
    {
        $r = 10;
        $Anonymous__o__q();
    };
    $r = 10;
    $p();
    $Anonymous__o__q();
};
