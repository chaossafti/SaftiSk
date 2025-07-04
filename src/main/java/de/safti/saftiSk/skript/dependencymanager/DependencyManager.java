package de.safti.saftiSk.skript.dependencymanager;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Config;
import ch.njol.util.OpenCloseable;
import de.safti.saftiSk.ConfigManager;
import de.safti.saftiSk.SaftiSk;
import de.safti.saftiSk.skript.SaftiLogHandler;
import de.safti.saftiSk.utils.DarkMagic;
import de.safti.saftiSk.utils.GithubAPIWrapper;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.github.GHRepository;
import org.skriptlang.skript.lang.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DependencyManager {
	private static final Logger log = LoggerFactory.getLogger(DependencyManager.class);
	
	private final ConfigManager configManager;
	private GithubAPIWrapper github;
	
	public static DependencyManager create(ConfigManager configManager) {
		if(SaftiSk.dependencyManager != null) {
			throw new IllegalStateException("DependencyManager already instantiated!");
		}
		
		try {
			return new DependencyManager(configManager, configManager.getToken());
		}
		catch (IOException e) {
			log.error("[&bDependencyManager&r] Could not connect to github", e);
		}
		
		return null;
	}
	
	public DependencyManager(ConfigManager configManager, String token) throws IOException {
		this.configManager = configManager;
		github = new GithubAPIWrapper(token);
		
		// register an event to mark dependencies using the DependencyScriptData class
		ScriptLoader.eventRegistry().register((ScriptLoader.ScriptLoadEvent) (parserInstance, script) -> {
			if(!script.getConfig().name().startsWith("github@")) return; // this isn't a dependency
			script.addData(new DependencyScriptData());
		});
		
	}

	public CompletableFuture<Set<DownloadedDependency>> downloadMissingDependencies() {
		Set<DownloadedDependency> result = new HashSet<>();
		var dependencyMap = configManager.getDependencies();
		final int beforeCleaningCount = dependencyMap.keySet()
				.stream()
				.map(dependencyMap::get)
				.mapToInt(Set::size)
				.sum();
		
		// remove already present dependencies
		for (String repo : dependencyMap.keySet()) {
			Set<String> files = dependencyMap.get(repo);
			files.removeIf(path -> proxyFileExists(repo, path));
		}
		
		// add up total deep size
		final int missingCount = dependencyMap.keySet()
				.stream()
				.map(dependencyMap::get)
				.mapToInt(Set::size)
				.sum();
		
		log.info("[&bDependencyManager&r] detected {} dependencies; downloading {} missing dependencies.", beforeCleaningCount, missingCount);
		
		if(missingCount == 0) {
			return CompletableFuture.completedFuture(new HashSet<>());
		}
		
		
		// the CompletableFuture to be returned
		CompletableFuture<Set<DownloadedDependency>> future = new CompletableFuture<>();
		
		// counter that counts each completed read-content request
		AtomicInteger counter = new AtomicInteger();
		
		
		// submit all tasks
		for (String repoName : dependencyMap.keySet()) {
			Set<String> files = dependencyMap.get(repoName);
			GHRepository repo = github.getRepo(repoName);
			
			// iterate each file dependency
			for (String path : files) {
				
				// submit every download task
				github.scheduleFileRead(repo, path, content -> {
					DownloadedDependency dependency = new DownloadedDependency(repoName, path, content);
					result.add(dependency);
					
					// complete the future if we're going processing the last read-request
					if(counter.incrementAndGet() == missingCount) {
						future.complete(result);
					}
				});
			}
		}
		
		
		return future;
	}
	
	
	public CompletableFuture<Set<DownloadedDependency>> downloadAll() {
		Set<DownloadedDependency> result = new HashSet<>();
		var dependencyMap = configManager.getDependencies();
		final int dependencyCount = dependencyMap.keySet()
				.stream()
				.map(dependencyMap::get)
				.mapToInt(Set::size)
				.sum();
		
		log.info("[&bDependencyManager&r] downloading {} dependencies", dependencyCount);
		
		if(dependencyCount == 0) {
			return CompletableFuture.completedFuture(new HashSet<>());
		}
		
		
		// the CompletableFuture to be returned
		CompletableFuture<Set<DownloadedDependency>> future = new CompletableFuture<>();
		
		// counter that counts each completed read-content request
		AtomicInteger counter = new AtomicInteger();
		
		
		// submit all tasks
		for (String repoName : dependencyMap.keySet()) {
			Set<String> files = dependencyMap.get(repoName);
			GHRepository repo = github.getRepo(repoName);
			
			// iterate each file dependency
			for (String path : files) {
				
				// submit every download task
				github.scheduleFileRead(repo, path, content -> {
					DownloadedDependency dependency = new DownloadedDependency(repoName, path, content);
					result.add(dependency);
					
					// complete the future if we're going processing the last read-request
					if(counter.incrementAndGet() == dependencyCount) {
						future.complete(result);
					}
				});
			}
		}
		
		
		return future;
	}
	
	public CompletableFuture<DownloadedDependency> download(String repoName, String path) {
		log.info("[&bDependencyManager&r] downloading 1 dependency");
		
		CompletableFuture<DownloadedDependency> future = new CompletableFuture<>();
		
		GHRepository repo = github.getRepo(repoName);
		github.scheduleFileRead(repo, path, content -> {
			future.complete(new DownloadedDependency(repoName, path, content));
		});
		
		return future;
	}
	
	public Set<Script> getLoadedDependencies() {
		return ScriptLoader.getLoadedScripts()
				.stream()
				.filter(script -> script.getData(DependencyScriptData.class) != null)
				.collect(Collectors.toUnmodifiableSet());
	}
	
	
	
	public void unloadAll() {
		ScriptLoader.unloadScripts(getLoadedDependencies());
	}
	
	public boolean proxyFileExists(String repo, String path) {
		return getProxyFile(repo, path, false).exists();
	}
	
	public void reloadAllScripts() {
		unloadAll();
		
		File dependencyDir = new File(SaftiSk.INSTANCE.getDataFolder(), "dependencies");
		List<Config> configs = new ArrayList<>();
		for (File repoDir : Objects.requireNonNullElseGet(dependencyDir.listFiles(), () -> new File[0])) {
			if(!repoDir.isDirectory()) {
				log.warn("[&bDependencyManager&r] Found non directory file in dependency dir; skipping");
				continue;
			}
			
			try (var stream = Files.walk(repoDir.toPath())) {
				Set<File> files = stream
						.filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".sk"))
						.map(Path::toFile)
						.collect(Collectors.toUnmodifiableSet());
				
				configs.addAll(filesToConfig(repoDir, files));
			}
			catch (IOException e) {
				log.error("[&bDependencyManager&r] Failed to walk through dependency directory", e);
				throw new RuntimeException(e);
			}
		}
		
		try (SaftiLogHandler logHandler = new SaftiLogHandler()) {
			log.info("[&bDependencyManager&r] loading {} dependencies", configs.size());
			CompletableFuture<ScriptLoader.ScriptInfo> future = loadScripts(configs, logHandler);
			
			future.thenAccept(scriptInfo -> {
				logHandler.printLog();
			});
		}
		
	}
	
	public void load(String repo, String... files) {
		File dependencyDir = new File(SaftiSk.INSTANCE.getDataFolder(), "dependencies");
		File repoFile = new File(dependencyDir, repo);
		Set<File> fileSet = Arrays.stream(files).map(path -> new File(repoFile, path)).collect(Collectors.toUnmodifiableSet());
		
		List<Config> configs = filesToConfig(repoFile, fileSet);
		try (SaftiLogHandler logHandler = new SaftiLogHandler()) {
			log.info("[&bDependencyManager&r] loading {} dependencies", configs.size());
			CompletableFuture<ScriptLoader.ScriptInfo> future = loadScripts(configs, logHandler);
			
			future.thenAccept(scriptInfo -> {
				logHandler.printLog();
			});
		}
		
	}
	
	private List<Config> filesToConfig(File repoFile, Set<File> files) {
		return files.stream()
				.map(file -> fileToConfig(repoFile, file))
				.toList();
	}
	
	private Config fileToConfig(File repoFile, File file) {
		try {
			InputStream inputStream = new FileInputStream(file);
			String relativePath = repoFile.toPath().relativize(file.toPath()).toString();
			return new Config(inputStream, formatName(repoFile.getName(), relativePath), file, true, false, ":");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void createProxyScripts(Set<DownloadedDependency> downloadedDependencies) {
		// turn all dependencies into Config's to be able to load them
		for (DownloadedDependency downloadedDependency : downloadedDependencies) {
			try {
				writeProxyFile(downloadedDependency);
			}
			catch (IOException e) {
				log.error("[&bDependencyManager&r] Failed to create proxy dependency skript file");
				throw new RuntimeException(e);
			}
		}
	}
	
	private CompletableFuture<ScriptLoader.ScriptInfo> loadScripts(List<Config> configs, OpenCloseable openCloseable) {
		DarkMagic.MethodInvocationInfo info = new DarkMagic.MethodInvocationInfo("loadScripts")
				.types(List.class, OpenCloseable.class)
				.args(configs, openCloseable);
		
		return DarkMagic.staticInvoke(ScriptLoader.class, info);
	}
	
	public File getProxyFile(String repoName, @NotNull String filePath, boolean createIfAbsent) {
		// make sure dependencies dir exist
		File pluginDir = SaftiSk.INSTANCE.getDataFolder();
		File dependenciesDir = new File(pluginDir, "dependencies");
		if(createIfAbsent) makeSureDirExists(dependenciesDir);
		
		// make sure repo dir exists
		File repoDir = new File(dependenciesDir, repoName);
		if(createIfAbsent) makeSureDirExists(repoDir);
		
		// create the proxy file
		String systemCompatiblePath = filePath.replace("/", FileSystems.getDefault().getSeparator());
		File proxyScriptFile = new File(repoDir, systemCompatiblePath);
		if(createIfAbsent) createFileIfAbsent(proxyScriptFile);
		
		return proxyScriptFile;
	}
	
	public void writeProxyFile(@NotNull DownloadedDependency downloadedDependency) throws IOException {
		File proxyScriptFile = getProxyFile(downloadedDependency.repoName(), downloadedDependency.filePath(), true);
		createFileIfAbsent(proxyScriptFile);
		
		// write the script content into the file
		Files.write(proxyScriptFile.toPath(), downloadedDependency.content().getBytes());
	}
	
	
	// github@author/repo;path/to/file.sk
	public String formatName(String repoName, String fileName) {
		return "github@%s;%s".formatted(repoName, fileName);
	}
	
	public void makeSureDirExists(File f) {
		if(!f.exists()) f.mkdir();
	}
	
	public void createFileIfAbsent(File f) {
		File parent = f.getParentFile();
		if(parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		
		try {
			f.createNewFile();
		}
		catch (IOException e) {
			log.error("[&bDependencyManager&r] Failed trying to create proxy script file {}", f.getPath());
			throw new RuntimeException(e);
		}
	}
	
	public void reload() {
		String token = github.getToken();
		String newToken = configManager.getToken();
		if(newToken.equals("anonymous")) newToken = null;
		
		if(Objects.equals(token, newToken)) return;
		github.setToken(newToken);
	}
}
