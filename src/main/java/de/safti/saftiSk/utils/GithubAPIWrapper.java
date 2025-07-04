package de.safti.saftiSk.utils;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AuthorizationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GithubAPIWrapper implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(GithubAPIWrapper.class);
	private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private GitHub github;
	private String token;
	
	public GithubAPIWrapper(String token) {
		try {
			this.github = new GitHubBuilder()
					.withAuthorizationProvider(() -> this.token == null ? AuthorizationProvider.ANONYMOUS.getEncodedAuthorization() : this.token)
					.build();
			this.token = token;
		}
		catch (IOException e) {
			log.error("Failed to connect to the Github api");
			throw new RuntimeException(e);
		}
		startWorker();
	}
	
	protected long getRateLimit() {
		return (long) (((token == null ? 60d : 5000d) / 3600)*1000);
	}
	
	protected String getFileContent(GHRepository repo, String filePath) throws IOException {
		GHContent content = repo.getFileContent(filePath);
		try(InputStream inputStream = content.read()) {
			return new String(inputStream.readAllBytes());
		}
	}
	
	public void scheduleFileRead(GHRepository repo, String filePath, Consumer<String> callback) {
		taskQueue.add(() -> {
			try {
				String content = getFileContent(repo, filePath);
				callback.accept(content);
			} catch (Exception e) {
				log.error("Failed reading content of file {} in repo {}", filePath, repo.getFullName(), e);
			}
		});
	}
	
	public GHRepository getRepo(String fullName) {
		try {
			return github.getRepository(fullName);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void startWorker() {
		if (running.compareAndSet(false, true)) {
			scheduler.scheduleWithFixedDelay(() -> {
				Runnable task = taskQueue.poll();
				if (task != null) {
					task.run();
				}
			}, 0, getRateLimit(), TimeUnit.MILLISECONDS);
		}
	}
	
	public void reload() {
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public void close() throws IOException {
		running.set(false);
		scheduler.shutdownNow();
		taskQueue.clear();
	}
}
