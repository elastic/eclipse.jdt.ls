package org.elastic.jdt.ls.core.internal;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.core.runtime.Platform;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;

/**
 * Watches the parent process PID and invokes exit if it is no longer available.
 * This implementation waits for periods of inactivity to start querying the PIDs.
 */
public final class ParentProcessWatcher implements Runnable, Function<MessageConsumer, MessageConsumer>{

	private static final long INACTIVITY_DELAY_SECS = 30 *1000;
	private static final int POLL_DELAY_SECS = 2;
	private volatile long lastActivityTime;
	private final LanguageServer server;
	private ScheduledFuture<?> task;
	private ScheduledExecutorService service;

	public ParentProcessWatcher(LanguageServer server ) {
		this.server = server;
		service = Executors.newScheduledThreadPool(1);
		task =  service.scheduleWithFixedDelay(this, POLL_DELAY_SECS, POLL_DELAY_SECS, TimeUnit.SECONDS);
	}

	@Override
	public void run() {
		if (!parentProcessStillRunning()) {
			task.cancel(true);
			server.exit();
		}
	}

	/**
	 * Checks whether the parent process is still running.
	 * If not, then we assume it has crashed, and we have to terminate the Java Language Server.
	 *
	 * @return true if the parent process is still running
	 */
	private boolean parentProcessStillRunning() {
		// Wait until parent process id is available
		final long pid = server.getParentProcessId();
		if (pid == 0 || lastActivityTime > (System.currentTimeMillis() - INACTIVITY_DELAY_SECS)) {
			return true;
		}
		String command;
		if (Platform.OS_WIN32.equals(Platform.getOS())) {
			command = "cmd /c \"tasklist /FI \"PID eq " + pid + "\" | findstr " + pid + "\"";
		} else {
			command = "ps -p " + pid;
		}
		try {
			Process process = Runtime.getRuntime().exec(command);
			int processResult = process.waitFor();
			return processResult == 0;
		} catch (IOException | InterruptedException e) {
			JavaLanguageServerPlugin.logException(e.getMessage(), e);
			return true;
		}
	}

	@Override
	public MessageConsumer apply(final MessageConsumer consumer) {
		//inject our own consumer to refresh the timestamp
		return message -> {
			lastActivityTime=System.currentTimeMillis();
			consumer.consume(message);
		};
	}
}
