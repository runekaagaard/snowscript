<?php

class TypeComparisonError extends Exception {};

function snow_eq($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a === $b;
}

function snow_neq($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a !== $b;
}

function snow_gt($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a > $b;
}

function snow_gte($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a >= $b;
}

function snow_lt($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a < $b;
}

function snow_lte($a, $b) {
	if (gettype($a) !== gettype($b)) {
    throw new TypeComparisonError(sprintf(
        "Cannot compare type %s with type %s",
        gettype($a), gettype($b)));
	}
	return $a <= $b;
}