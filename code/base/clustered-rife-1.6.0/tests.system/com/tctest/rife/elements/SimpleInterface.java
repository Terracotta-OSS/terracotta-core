package com.tctest.rife.elements;

import com.uwyn.rife.engine.ElementAware;
import com.uwyn.rife.engine.ElementSupport;
import com.uwyn.rife.engine.annotations.Elem;

@Elem
public class SimpleInterface implements ElementAware, Cloneable {
	private ElementSupport mElement;

	public void noticeElement(ElementSupport element) {
		mElement = element;
	}

	public void processElement() {
		String before = "before simple pause";
		String after = "after simple pause";

		mElement.print(before + "\n" + mElement.getContinuationId());
		mElement.pause();
		mElement.print(after);
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
