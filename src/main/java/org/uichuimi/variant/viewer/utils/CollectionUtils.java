package org.uichuimi.variant.viewer.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtils {

	private static final String NULL_VALUE = ".";


	private CollectionUtils() {}


	public static <T, R> List<R> map(Function<T, R> mapper, List<T> list) {
		return list.stream().map(mapper).collect(Collectors.toList());
	}

	public static <T, R> List<R> map(Function<T, R> mapper, T... list) {
		return Arrays.stream(list).map(mapper).collect(Collectors.toList());
	}

	public static <T> List<T> filter(Predicate<T> filter, Collection<T> collection) {
		return collection.stream().filter(filter).collect(Collectors.toList());
	}

	public static Stream<?> stream(Object object, boolean nullify) {
		return nullify
				? stream(object).map(o -> o.equals(NULL_VALUE) ? null : o)
				: stream(object);
	}

	public static Stream<?> stream(Object object) {
		if (object == null) {
			return Stream.empty();
		}
		if (object instanceof Collection) {
			return ((Collection<?>) object).stream();
		}
		if (object instanceof Object[]) {
			return Arrays.stream((Object[]) object);
		}
		if (object instanceof int[]) {
			return Arrays.stream((int[]) object).boxed();
		}
		if (object instanceof double[]) {
			return Arrays.stream((double[]) object).boxed();
		}
		return Stream.of(object);
	}

	public static List<?> toList(Object object) {
		return stream(object).collect(Collectors.toList());
	}

	public static List<?> toList(Object object, boolean nullify) {
		return stream(object, nullify).collect(Collectors.toList());
	}

	public static Object first(Object object) {
		return stream(object).findFirst().orElse(null);
	}

	public static List<?> skip(int skip, Object object) {
		return stream(object).skip(skip).collect(Collectors.toList());
	}


}
