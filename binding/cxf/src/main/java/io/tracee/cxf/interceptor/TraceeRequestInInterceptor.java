package io.tracee.cxf.interceptor;

import io.tracee.Tracee;
import io.tracee.TraceeBackend;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;

import static io.tracee.configuration.TraceeFilterConfiguration.Channel.IncomingResponse;

public class TraceeRequestInInterceptor extends AbstractTraceeInInterceptor {

	public TraceeRequestInInterceptor() {
		this(Tracee.getBackend(), null);
	}

	public TraceeRequestInInterceptor(TraceeBackend backend) {
		this(backend, null);
	}

	public TraceeRequestInInterceptor(TraceeBackend backend, String profile) {
		super(Phase.PRE_INVOKE, IncomingResponse, backend, profile);
	}

	@Override
	protected boolean shouldHandleMessage(Message message) {
		return !MessageUtils.isRequestor(message);
	}
}