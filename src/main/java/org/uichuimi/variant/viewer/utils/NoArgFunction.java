package org.uichuimi.variant.viewer.utils;

@FunctionalInterface
public interface NoArgFunction {

	NoArgFunction NO_OP = () -> {
	};

	void call();

}
