package com.tctest.rife.elements;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.ParamProperty;
import com.uwyn.rife.engine.annotations.Submission;
import com.uwyn.rife.template.Template;

@Elem(submissions = {@Submission(name = "getanswer") })
public class StepBack extends Element {
	private int mTotal = -5;
	private boolean mStart = false;
	private int mAnswer;

	@ParamProperty
	public void setStart(boolean start) {
		mStart = start;
	}

	@ParamProperty
	public void setAnswer(int answer) {
		mAnswer = answer;
	}

	public void processElement() {
		Template template = getHtmlTemplate("stepback");

		if (mTotal < 0) {
			mTotal++;
			stepBack();
		}

		template.setValue("stepback", duringStepBack());
		print(template);
		pause();

		if (mStart) {
			template.setValue("subtotal", mTotal);
			template.setValue("stepback", duringStepBack());
			print(template);
			pause();
			mTotal += mAnswer;

			if (mTotal < 50) {
				stepBack();
			}
		}

		template.setValue("subtotal", "got a total of " + mTotal);
		template.setValue("stepback", duringStepBack());
		print(template);
	}
}
