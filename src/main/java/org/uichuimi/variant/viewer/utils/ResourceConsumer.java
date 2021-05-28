package org.uichuimi.variant.viewer.utils;

public interface ResourceConsumer<Header, Content> {

	void start(Header header);

	void consume(Content content, long position);

	void finnish(long position) throws Exception;
}
