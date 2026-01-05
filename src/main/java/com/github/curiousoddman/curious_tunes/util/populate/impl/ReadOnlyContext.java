package com.github.curiousoddman.curious_tunes.util.populate.impl;

import java.util.Deque;
import java.util.List;

public interface ReadOnlyContext {
	ValueGenerator<?>[] getCustomGenerators();

	int getSeed();

	List<String> getPath();

	Class<?> getCurrentParameterClass();

	String getCurrentMethodName();

	Class<?> getCurrentGenericType();

	Deque<PathElement> getPathElements();
}
