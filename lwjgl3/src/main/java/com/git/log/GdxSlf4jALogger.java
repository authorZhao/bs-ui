package com.git.log;

import com.badlogic.gdx.ApplicationLogger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GdxSlf4jALogger implements ApplicationLogger {

	@Override
	public void log (String tag, String message) {
        log.info("[{}] {}", tag, message);
	}

	@Override
	public void log (String tag, String message, Throwable exception) {
		log.error("[{}] {} ", tag, message, exception);
	}

	@Override
	public void error (String tag, String message) {
		log.error("[{}] {}", tag, message);
	}

	@Override
	public void error (String tag, String message, Throwable exception) {
		log.error("[{}] {} ", tag, message, exception);
	}

	@Override
	public void debug (String tag, String message) {
		log.debug("[{}] {}", tag, message);
	}

	@Override
	public void debug (String tag, String message, Throwable exception) {
		log.error("[{}] {} ", tag, message, exception);
	}
}
