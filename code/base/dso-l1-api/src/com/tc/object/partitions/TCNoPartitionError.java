package com.tc.object.partitions;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCError;

public class TCNoPartitionError extends TCError{
	  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
	  
	  public TCNoPartitionError(String message) {
	    super(wrapper.wrap(message));
	  }
}
