package com.tctest.rife.elements;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.InputProperty;

@Elem
public class CallAnswerExit extends Element {
	private String someData;

	@InputProperty
	public void setSomeData(String someData) {
		this.someData = someData;
	}
	
	public void processElement() {
		print("the data:" + someData + "\n");
		print("before answer\n");
		answer("the exit's answer\n");
		print("after answer");
	}
}
