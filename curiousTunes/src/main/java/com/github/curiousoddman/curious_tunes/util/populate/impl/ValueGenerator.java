package com.github.curiousoddman.curious_tunes.util.populate.impl;

public interface ValueGenerator<T> {
	boolean isApplicable(ReadOnlyContext context);

	T generateValue(ReadOnlyContext context);
}
