package cpath.service;

import java.util.Arrays;

import cpath.service.api.Status;
import cpath.service.jaxb.ServiceResponse;

public final class ErrorResponse extends ServiceResponse {
	private final Status status;
    private String errorDetails;
    
    public ErrorResponse(Status status, Object o) {
		this.status = status;
    	
    	String msg = null;
		if(o instanceof Throwable) {
			msg = o.toString() + "; " + Arrays.toString(((Throwable)o).getStackTrace());
		} else {
			if(o != null)
				msg = o.toString();
		}
		this.errorDetails = msg;
	}
    
    public Integer getErrorCode() {
        return status.getCode();
    }


    public String getErrorMsg() {
        return status.getMsg();
    }


    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String value) {
        this.errorDetails = value;
    }
	

	public boolean isStatus(Status statusCode) {
		return statusCode.equals(status);
	}
	
	public Status getStatus() {
		return status;
	}
	
	@Override
	public String toString() {
		return 	getErrorMsg() + " - " + errorDetails;
	}


	@Override
	public boolean isEmpty() {
		return false;
	}
	
}
