package com.tctest.rife.elements;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Autolink;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.OutputProperty;

@Elem(autolinks = {@Autolink(srcExit = "exit", destClass = CallAnswerExit.class)})
public class CallAnswer extends Element {
	private String someData;
	
	@OutputProperty
	public String getSomeData() {
		return someData;
	}
	
	public void processElement() {
		String before = "before call";
		String after = "after call";

		print(before + "\n" + getContinuationId() + "\n");
		someData = "somevalue";
		print(call("exit"));
		print(after);
	}
}