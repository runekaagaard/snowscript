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

class SnowList extends ArrayObject {
	function get($i) { return $this[$i]; }

	// Add an item to the end of the list
	function append($x) { parent::append($x); return $this; }

	// Extend the list by appending all the items in the given list
	function extend($xs) { foreach($xs as $x) parent::append($x); return $this; }

	// Insert an item at a given position. The first argument is the index of 
	// the element before which to insert
	function insert($i, $x) { 
		throw new Exception("TODO");
	}

	// Remove the item at the given position in the list, and return it. If no 
	// index is specified, a.pop() removes and returns the last item in the list.
	function pop($i=null) { 
		if ($i === null) {
			$i = $this->count() - 1;
			if ($i === 0) throw new Exception("Can't pop from empty list.");
		}
		$x = $this[$i];
		unset($this[$i]);
		return $x;
	}
}

class SnowDict extends ArrayObject {

}

function snow_list($array) {
	return new SnowList($array);
}

function snow_dict($array) {
	return new SnowDict($array, ArrayObject::ARRAY_AS_PROPS);
}