/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.tctest.spring.bean;

//import java.util.Calendar;
import java.io.Serializable;
import java.util.Random;

/**
 * Adapded from SWF numberguess example (made 1.4 compliant and non Serializable).
 * 
 * Action that encapsulates logic for the number guess sample flow. Note that
 * this is a stateful action: it holds modifiable state in instance members!
 * 
 * @author Erwin Vervaet
 * @author Keith Donald
 */
public class HigherLowerGame implements Serializable {  // TODO required by SerializedFlowExecutionContinuation

	public static final Random random = new Random();

  public static final String INVALID = "invalid";

  public static final String TOO_HIGH = "toohigh";

  public static final String TOO_LOW = "toolow";

  public static final String CORRECT = "corrent";

//	private Calendar start = Calendar.getInstance();
	private long start = System.currentTimeMillis();

	private int answer = random.nextInt(101);

	private int guesses = 0;

	private String result;
  

	public int getAnswer() {
		return answer;
	}

	public int getGuesses() {
		return guesses;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public long getDuration() {
//		Calendar now = Calendar.getInstance();
//	  long durationMilliseconds = now.getTime().getTime() - start.getTime().getTime();
//	  return durationMilliseconds / 1000;
		long now = System.currentTimeMillis();
    return now - start;
	}

	public String makeGuess(int guess) {
		if (guess < 0 || guess > 100) {
			setResult(INVALID);
		}
		else {
			guesses++;
			if (answer < guess) {
				setResult(TOO_HIGH);
			}
			else if (answer > guess) {
				setResult(TOO_LOW);
			}
			else {
				setResult(CORRECT);
			}
		}
		return getResult();
	}

}

