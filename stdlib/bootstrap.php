<?php

require dirname(__FILE__) . '/data_structures.php';

class TypeComparisonError extends Exception {};
class AssertionError extends Exception {};
class KeyError extends Exception {};
class IndexError extends Exception {};

function assert_type_eq($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
}

function assert_type_numeric($a, $b) {
	$type_a = gettype($a);
	if ($type_a !== "integer" || $type_a !== "float") {
		throw new TypeComparisonError("Must be numeric.");
	}
	$type_b = gettype($a);
	if ($type_b !== "integer" || $type_b !== "float") {
		throw new TypeComparisonError("Must be numeric.");
	}
}

function snow_eq($a, $b) {
	assert_type_eq($a, $b);
	return $a === $b;
}

function snow_neq($a, $b) {
	assert_type_eq($a, $b);
	return $a !== $b;
}

function snow_gt($a, $b) {
	assert_type_eq($a, $b);
	assert_type_numeric($a, $b);
	return $a > $b;
}

function snow_gte($a, $b) {
	assert_type_eq($a, $b);
	assert_type_numeric($a, $b);
	return $a >= $b;
}

function snow_lt($a, $b) {
	assert_type_eq($a, $b);
	assert_type_numeric($a, $b);
	return $a < $b;
}

function snow_lte($a, $b) {
	assert_type_eq($a, $b);
	assert_type_numeric($a, $b);
	return $a <= $b;
}

function asrt($assertion) {
    if (!$assertion)
        throw new AssertionError();
}

function snow_list($array) {
	return new SnowList($array);
}

function snow_dict($array) {
	return new SnowDict($array, ArrayObject::ARRAY_AS_PROPS);
}